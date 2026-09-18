package de.deringo.forgemind.core.tool;

public final class ToolResultTruncator {

    private static final int MAX_LENGTH = 8_000;
    private static final int HEAD_LENGTH = 3_000;
    private static final int TAIL_LENGTH = 4_000;

    private ToolResultTruncator() {
    }

    public static ToolResult truncate(ToolResult result) {

        String content = result.content();

        if (content == null || content.length() <= MAX_LENGTH) {
            return result;
        }

        int omitted =
                content.length() - HEAD_LENGTH - TAIL_LENGTH;

        String truncated =
                content.substring(0, HEAD_LENGTH)
                + "\n\n"
                + "... ["
                + omitted
                + " characters omitted] ..."
                + "\n\n"
                + content.substring(
                        content.length() - TAIL_LENGTH
                );

        return new ToolResult(truncated);
    }
}