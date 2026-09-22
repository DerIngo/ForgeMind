package de.deringo.forgemind.core.patch;

import java.util.ArrayList;
import java.util.List;

public final class PatchParser {

    private static final String BEGIN_PATCH =
            "*** Begin Patch";

    private static final String END_PATCH =
            "*** End Patch";

    private static final String UPDATE_FILE =
            "*** Update File: ";

    private static final String HUNK =
            "@@";

    public List<FilePatch> parse(
            String patch
    ) {
        if (patch == null || patch.isBlank()) {
            throw new IllegalArgumentException(
                    "Patch must not be blank."
            );
        }

        List<String> lines =
                patch.lines().toList();

        if (lines.isEmpty()
                || !lines.getFirst().equals(BEGIN_PATCH)) {
            throw new IllegalArgumentException(
                    "Patch must start with "
                            + BEGIN_PATCH
            );
        }

        if (!lines.getLast().equals(END_PATCH)) {
            throw new IllegalArgumentException(
                    "Patch must end with "
                            + END_PATCH
            );
        }

        List<FilePatch> filePatches =
                new ArrayList<>();

        int index = 1;

        while (index < lines.size() - 1) {

            String line =
                    lines.get(index);

            if (line.isBlank()) {
                index++;
                continue;
            }

            if (!line.startsWith(UPDATE_FILE)) {
                throw new IllegalArgumentException(
                        "Expected file header at line "
                                + (index + 1)
                                + ": "
                                + line
                );
            }

            String path =
                    line.substring(
                            UPDATE_FILE.length()
                    ).trim();

            if (path.isBlank()) {
                throw new IllegalArgumentException(
                        "Patch file path must not be blank."
                );
            }

            index++;

            List<PatchHunk> hunks =
                    new ArrayList<>();

            while (index < lines.size() - 1
                    && !lines.get(index)
                            .startsWith(UPDATE_FILE)) {

                line = lines.get(index);

                if (line.isBlank()) {
                    index++;
                    continue;
                }

                if (!line.equals(HUNK)) {
                    throw new IllegalArgumentException(
                            "Expected @@ at line "
                                    + (index + 1)
                                    + ": "
                                    + line
                    );
                }

                index++;

                List<String> oldLines =
                        new ArrayList<>();

                List<String> newLines =
                        new ArrayList<>();

                while (index < lines.size() - 1) {

                    line = lines.get(index);

                    if (line.equals(HUNK)
                            || line.startsWith(UPDATE_FILE)) {
                        break;
                    }

                    if (line.startsWith(" ")) {
                        String content =
                                line.substring(1);

                        oldLines.add(content);
                        newLines.add(content);

                    } else if (line.startsWith("-")) {

                        oldLines.add(
                                line.substring(1)
                        );

                    } else if (line.startsWith("+")) {

                        newLines.add(
                                line.substring(1)
                        );

                    } else {

                        throw new IllegalArgumentException(
                                "Invalid patch line at "
                                        + (index + 1)
                                        + ": "
                                        + line
                        );
                    }

                    index++;
                }

                if (oldLines.isEmpty()
                        && newLines.isEmpty()) {
                    throw new IllegalArgumentException(
                            "Patch hunk must not be empty."
                    );
                }

                hunks.add(
                        new PatchHunk(
                                List.copyOf(oldLines),
                                List.copyOf(newLines)
                        )
                );
            }

            if (hunks.isEmpty()) {
                throw new IllegalArgumentException(
                        "No hunks found for file: "
                                + path
                );
            }

            filePatches.add(
                    new FilePatch(
                            path,
                            List.copyOf(hunks)
                    )
            );
        }

        if (filePatches.isEmpty()) {
            throw new IllegalArgumentException(
                    "Patch contains no file changes."
            );
        }

        return List.copyOf(filePatches);
    }
}
