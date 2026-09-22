package de.deringo.forgemind.core.patch;

import de.deringo.forgemind.core.workspace.ProjectWorkspace;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PatchApplier {
    private final ProjectWorkspace workspace;

    public PatchApplier(ProjectWorkspace workspace) {
        this.workspace = workspace;
    }

    public void apply(List<FilePatch> patches) {
        if (patches == null || patches.isEmpty()) {
            throw new IllegalArgumentException("Patches must not be empty.");
        }
        Map<Path, String> pendingWrites = new LinkedHashMap<>();
        Map<Path, String> workspacePaths = new LinkedHashMap<>();
        for (FilePatch patch : patches) {
            Path path = workspace.resolveExistingFile(patch.path());
            workspacePaths.put(path, patch.path());
            String original = pendingWrites.containsKey(path)
                    ? pendingWrites.get(path) : workspace.readFile(patch.path());
            if (original.indexOf('\0') >= 0) {
                throw new IllegalArgumentException("Not a text file: " + patch.path());
            }
            pendingWrites.put(path, applyFilePatch(original, patch));
        }
        // Validate every file before writing. I/O failures are not transactional.
        for (Map.Entry<Path, String> write : pendingWrites.entrySet()) {
            workspace.writeFile(workspacePaths.get(write.getKey()), write.getValue());
        }
    }

    private String applyFilePatch(String content, FilePatch patch) {
        String separator = content.contains("\r\n") ? "\r\n" : "\n";
        String normalized = content.replace("\r\n", "\n");
        boolean trailingNewline = normalized.endsWith("\n");
        List<String> lines = new ArrayList<>(normalized.lines().toList());
        for (PatchHunk hunk : patch.hunks()) {
            if (hunk.oldLines().isEmpty()) {
                throw new IllegalArgumentException("Patch additions require context: " + patch.path());
            }
            int match = -1;
            for (int i = 0; i <= lines.size() - hunk.oldLines().size(); i++) {
                if (lines.subList(i, i + hunk.oldLines().size()).equals(hunk.oldLines())) {
                    if (match >= 0) {
                        throw new IllegalArgumentException("Patch hunk is ambiguous in file: " + patch.path());
                    }
                    match = i;
                }
            }
            if (match < 0) {
                throw new IllegalArgumentException("Patch hunk does not match file: " + patch.path());
            }
            lines.subList(match, match + hunk.oldLines().size()).clear();
            lines.addAll(match, hunk.newLines());
        }
        return String.join(separator, lines)
                + (trailingNewline && !lines.isEmpty() ? separator : "");
    }
}
