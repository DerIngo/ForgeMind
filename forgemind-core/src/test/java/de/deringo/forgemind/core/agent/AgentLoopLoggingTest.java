package de.deringo.forgemind.core.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import de.deringo.forgemind.core.llm.LlmClient;
import de.deringo.forgemind.core.llm.LlmResponse;
import de.deringo.forgemind.core.permission.Capability;
import de.deringo.forgemind.core.permission.InMemoryPermissionStore;
import de.deringo.forgemind.core.permission.PermissionDecision;
import de.deringo.forgemind.core.permission.PermissionGrant;
import de.deringo.forgemind.core.tool.AgentTool;
import de.deringo.forgemind.core.tool.ToolCall;
import de.deringo.forgemind.core.tool.ToolDefinition;
import de.deringo.forgemind.core.tool.ToolRegistry;
import de.deringo.forgemind.core.tool.ToolResult;

class AgentLoopLoggingTest {
    @ParameterizedTest
    @EnumSource(value = PermissionDecision.class, names = {"ASK", "DENY"})
    void deniedToolsAreReportedWithoutExecution(PermissionDecision decision) {
        runCase(decision, "unused");
    }

    @Test void observerReceivesFullOutputBeforeLlmTruncation() {
        runCase(PermissionDecision.ALLOW, "Exit code: 1\n\nOutput:\n" + "error\n".repeat(20000));
    }

    private void runCase(PermissionDecision decision, String content) {
        var executed = new AtomicInteger();
        var prompts = new AtomicInteger();
        var turns = new AtomicInteger();
        var results = new ArrayList<ToolResult>();
        var registry = new ToolRegistry();
        registry.register(new AgentTool() {
            public ToolDefinition definition() { return new ToolDefinition("run_command", "test", Map.of()); }
            public Capability capability() { return Capability.EXECUTE; }
            public ToolResult execute(Map<String, Object> args) {
                executed.incrementAndGet();
                return new ToolResult(content);
            }
        });
        String expected = decision == PermissionDecision.DENY ? "Permission denied by policy."
                : "Permission denied by user.";
        LlmClient client = request -> {
            if (turns.getAndIncrement() == 0) {
                return new LlmResponse(null, List.of(new ToolCall("call", "run_command", Map.of())));
            }
            var message = request.messages().getLast();
            assertEquals("call", message.toolCallId());
            if (decision == PermissionDecision.ALLOW) {
                assertTrue(message.content().length() < content.length());
            } else {
                assertEquals(expected, message.content());
            }
            return new LlmResponse("done", List.of());
        };
        var loop = new AgentLoop(client, "test", () -> "test", registry, new AgentObserver() {
            public void onToolResult(ToolCall call, ToolResult result, Duration duration) {
                assertEquals("call", call.id());
                results.add(result);
            }
        }, request -> decision, request -> {
            prompts.incrementAndGet();
            return PermissionGrant.deny();
        }, new InMemoryPermissionStore());
        assertEquals("done", loop.run("test"));
        assertEquals(1, results.size());
        assertEquals(decision == PermissionDecision.ALLOW ? content : expected, results.getFirst().content());
        assertEquals(decision == PermissionDecision.ALLOW ? 1 : 0, executed.get());
        assertEquals(decision == PermissionDecision.ASK ? 1 : 0, prompts.get());
    }
}
