package de.deringo.forgemind.core.patch;

import de.deringo.forgemind.core.workspace.ProjectWorkspace;
import de.deringo.forgemind.core.tool.filesystem.ApplyPatchTool;
import de.deringo.forgemind.core.permission.Capability;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class PatchTest {
    @TempDir Path root;
    private final PatchParser parser = new PatchParser();

    private String patch(String body) {
        return "*** Begin Patch\n" + body + "\n*** End Patch\n";
    }

    private void apply(String body) {
        new PatchApplier(new ProjectWorkspace(root)).apply(parser.parse(patch(body)));
    }

    @ParameterizedTest
    @CsvSource({"old,new", "xold,old", "oldx,old"})
    void replacementMatchesWholeLines(String original, String replacement) throws Exception {
        Files.writeString(root.resolve("a.txt"), original + "\n");
        apply("*** Update File: a.txt\n@@\n-" + original + "\n+" + replacement);
        assertEquals(replacement + "\n", Files.readString(root.resolve("a.txt")));
    }

    @Test void additionsDeletionsAndMultipleHunks() throws Exception {
        Files.writeString(root.resolve("a.txt"), "first\nremove\nlast\n");
        apply("*** Update File: a.txt\n@@\n first\n+added\n@@\n-remove\n last");
        assertEquals("first\nadded\nlast\n", Files.readString(root.resolve("a.txt")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"\n", "\r\n"})
    void preservesLineEndings(String separator) throws Exception {
        Files.writeString(root.resolve("a.txt"), "old" + separator + "end" + separator);
        apply("*** Update File: a.txt\n@@\n-old\n+new\n end");
        assertEquals("new" + separator + "end" + separator, Files.readString(root.resolve("a.txt")));
    }

    @Test void multipleFilesAndRepeatedFileSections() throws Exception {
        Files.writeString(root.resolve("a.txt"), "old");
        Files.writeString(root.resolve("b.txt"), "old\n");
        apply("*** Update File: a.txt\n@@\n-old\n+new\n*** Update File: b.txt\n@@\n-old\n+new\n*** Update File: ./a.txt\n@@\n-new\n+final");
        assertEquals("final", Files.readString(root.resolve("a.txt")));
        assertEquals("new\n", Files.readString(root.resolve("b.txt")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing", "old\nold\n", "prefixold\n", "oldSuffix\n"})
    void missingAmbiguousOrPartialMatchDoesNotWrite(String original) throws Exception {
        Files.writeString(root.resolve("a.txt"), original);
        assertThrows(IllegalArgumentException.class, () -> apply("*** Update File: a.txt\n@@\n-old\n+new"));
        assertEquals(original, Files.readString(root.resolve("a.txt")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing.txt", "../outside.txt", "a.txt"})
    void validatesAllFilesBeforeWriting(String second) throws Exception {
        Files.writeString(root.resolve("a.txt"), "old\n");
        assertThrows(IllegalArgumentException.class, () -> apply("*** Update File: a.txt\n@@\n-old\n+new\n*** Update File: " + second + "\n@@\n-missing\n+new"));
        assertEquals("old\n", Files.readString(root.resolve("a.txt")));
        assertFalse(Files.exists(root.resolve("missing.txt")));
    }

    @Test void deletionRemovesEntireLine() throws Exception {
        Files.writeString(root.resolve("a.txt"), "old\n");
        apply("*** Update File: a.txt\n@@\n-old");
        assertEquals("", Files.readString(root.resolve("a.txt")));
    }

    @Test void contextFreeAdditionIsRejected() throws Exception {
        Files.writeString(root.resolve("a.txt"), "old");
        assertThrows(IllegalArgumentException.class, () -> apply("*** Update File: a.txt\n@@\n+new"));
        assertEquals("old", Files.readString(root.resolve("a.txt")));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"bad", "*** Begin Patch\n*** End Patch", "*** Begin Patch\n*** Update File: a\n*** End Patch", "*** Begin Patch\n*** Update File: a\n@@\nbad\n*** End Patch", "*** Begin Patch\n*** Update File: a\n@@\n*** End Patch", "*** Begin Patch\n*** Add File: a\n+x\n*** End Patch"})
    void rejectsMalformedPatch(String value) {
        assertThrows(IllegalArgumentException.class, () -> parser.parse(value));
    }

    @Test void toolUsesSortedPermissionResourceAndExecutes() throws Exception {
        var tool = new ApplyPatchTool(parser, new PatchApplier(new ProjectWorkspace(root)));
        var arguments = Map.<String, Object>of("patch", patch("*** Update File: b.txt\n@@\n-old\n+new\n*** Update File: a.txt\n@@\n-old\n+new"));
        assertEquals(Capability.WRITE, tool.capability());
        assertEquals(new de.deringo.forgemind.core.permission.PermissionKey(Capability.WRITE, "apply_patch", "a.txt, b.txt"), tool.permissionKey(arguments));
        Files.writeString(root.resolve("a.txt"), "old");
        Files.writeString(root.resolve("b.txt"), "old");
        assertNotNull(tool.execute(arguments));
        assertEquals("new", Files.readString(root.resolve("a.txt")));
        assertEquals("new", Files.readString(root.resolve("b.txt")));
        assertDoesNotThrow(() -> tool.permissionKey(Map.of("patch", "invalid")));
        assertDoesNotThrow(() -> tool.permissionKey(Map.of()));
    }

    @Test void workspaceWriteRequiresExistingFile() throws Exception {
        var workspace = new ProjectWorkspace(root);
        assertThrows(IllegalArgumentException.class, () -> workspace.writeFile("missing.txt", "new"));
        workspace.createFile("a.txt", "old");
        workspace.writeFile("a.txt", "new");
        assertEquals("new", workspace.readFile("a.txt"));
        assertThrows(IllegalArgumentException.class, () -> workspace.createFile("a.txt", "other"));
    }
}
