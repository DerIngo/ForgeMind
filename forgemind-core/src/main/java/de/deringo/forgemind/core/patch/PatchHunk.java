package de.deringo.forgemind.core.patch;

import java.util.List;

public record PatchHunk(
        List<String> oldLines,
        List<String> newLines
) {
}
