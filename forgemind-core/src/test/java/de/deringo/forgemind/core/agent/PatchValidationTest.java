package de.deringo.forgemind.core.agent;

import de.deringo.forgemind.core.llm.*;
import de.deringo.forgemind.core.patch.*;
import de.deringo.forgemind.core.permission.*;
import de.deringo.forgemind.core.tool.*;
import de.deringo.forgemind.core.tool.filesystem.ApplyPatchTool;
import de.deringo.forgemind.core.workspace.ProjectWorkspace;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class PatchValidationTest {
    @TempDir Path root;

    private String patch(String body) {
        return "*** Begin Patch\n*** Update File: a.txt\n" + body + "\n*** End Patch";
    }

    @Test void formatErrorsExplainHowToRepairPatch() {
        var parser = new PatchParser();
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> parser.parse("*** Begin Patch\n*** Update File: a.txt\n@@\n-old\n+new"))
                .getMessage().contains("Resend the complete patch"));
        for (String body : List.of("@@ -1,1 +1,1 @@\n-old\n+new",
                "@@\n-old\n+new\n@@ -2,1 +2,1 @@\n-end\n+done")) {
            assertTrue(assertThrows(IllegalArgumentException.class, () -> parser.parse(patch(body)))
                    .getMessage().contains("line numbers and labels are not supported"));
        }
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> parser.parse(patch("@@\n-old\n\n+new")))
                .getMessage().contains("single space"));
        var parsed = parser.parse(patch("@@\n \n-old\n+new\n+"));
        assertEquals(List.of("", "old"), parsed.getFirst().hunks().getFirst().oldLines());
        assertEquals(List.of("", "new", ""), parsed.getFirst().hunks().getFirst().newLines());
    }

    @Test void invalidArgumentsAreRejectedWithoutWriting() throws Exception {
        Files.writeString(root.resolve("a.txt"), "old\n");
        var tool = new ApplyPatchTool(new PatchParser(), new PatchApplier(new ProjectWorkspace(root)));
        for (Map<String, Object> args : List.of(Map.<String, Object>of(), Map.<String, Object>of("patch", 42),
                Map.<String, Object>of("patch", ""))) {
            assertThrows(IllegalArgumentException.class, () -> tool.validateArguments(args));
            assertThrows(IllegalArgumentException.class, () -> tool.execute(args));
        }
        tool.validateArguments(Map.of("patch", patch("@@\n-old\n+new")));
        assertEquals("old\n", Files.readString(root.resolve("a.txt")));
    }

    @Test void malformedPatchSkipsPermissionAndCorrectedRetryRequiresPermission() throws Exception {
        Files.writeString(root.resolve("a.txt"), "old\n");
        var registry = new ToolRegistry();
        registry.register(new ApplyPatchTool(new PatchParser(), new PatchApplier(new ProjectWorkspace(root))));
        var calls = new AtomicInteger();
        var evaluations = new AtomicInteger();
        var permissions = new AtomicInteger();
        var observed = new ArrayList<ToolResult>();
        LlmClient client = request -> {
            int step = calls.getAndIncrement();
            if (step == 0) {
                return new LlmResponse(null, List.of(new ToolCall("bad", "apply_patch", Map.of("patch", "*** Begin Patch"))));
            }
            if (step == 1) {
                var error = request.messages().getLast();
                assertEquals(LlmMessage.Role.TOOL, error.role());
                assertEquals("bad", error.toolCallId());
                assertTrue(error.content().contains("*** End Patch"));
                assertEquals(0, evaluations.get());
                assertEquals(0, permissions.get());
                return new LlmResponse(null, List.of(new ToolCall("fixed", "apply_patch", Map.of("patch", patch("@@\n-old\n+new")))));
            }
            assertTrue(request.messages().getLast().content().contains("Patch applied successfully"));
            return new LlmResponse("done", List.of());
        };
        var agent = new AgentLoop(client, "test", () -> "test", registry, new AgentObserver() {
            @Override public void onToolResult(ToolCall call, ToolResult result, Duration duration) {
                observed.add(result);
            }
        }, request -> {
            evaluations.incrementAndGet();
            return PermissionDecision.ASK;
        }, request -> {
            permissions.incrementAndGet();
            assertEquals(new PermissionKey(Capability.WRITE, "apply_patch", "a.txt"), request.key());
            return PermissionGrant.allow(PermissionScope.ONCE);
        }, new InMemoryPermissionStore());
        assertEquals("done", agent.run("change file"));
        assertEquals(1, evaluations.get());
        assertEquals(1, permissions.get());
        assertEquals(2, observed.size());
        assertEquals("new\n", Files.readString(root.resolve("a.txt")));
    }
}
