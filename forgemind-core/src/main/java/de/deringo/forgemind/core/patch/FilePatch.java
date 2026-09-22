package de.deringo.forgemind.core.patch;

import java.util.List;

public record FilePatch(
        String path,
        List<PatchHunk> hunks
) {
}
