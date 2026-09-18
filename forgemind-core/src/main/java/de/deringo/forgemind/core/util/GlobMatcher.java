package de.deringo.forgemind.core.util;

public final class GlobMatcher {

    private GlobMatcher() {
    }

    public static boolean matches(
            String pattern,
            String path
    ) {

        String normalizedPattern =
                normalizePattern(pattern);

        String normalizedPath =
                path.replace('\\', '/');

        return normalizedPath.matches(
                toRegex(normalizedPattern)
        );
    }

    private static String normalizePattern(
            String pattern
    ) {

        String normalized =
                pattern.replace('\\', '/');

        if (!normalized.contains("/")) {
            normalized = "**/" + normalized;
        }

        return normalized;
    }

    private static String toRegex(
            String glob
    ) {

        StringBuilder regex =
                new StringBuilder("^");

        for (int i = 0; i < glob.length(); i++) {

            char c = glob.charAt(i);

            if (c == '*') {

                if (i + 1 < glob.length()
                        && glob.charAt(i + 1) == '*') {

                    i++;

                    if (i + 1 < glob.length()
                            && glob.charAt(i + 1) == '/') {

                        regex.append("(?:.*/)?");
                        i++;

                    } else {
                        regex.append(".*");
                    }

                } else {
                    regex.append("[^/]*");
                }

            } else if (c == '?') {

                regex.append("[^/]");

            } else {

                if (".+()^$|{}[]\\".indexOf(c) >= 0) {
                    regex.append('\\');
                }

                regex.append(c);
            }
        }

        regex.append('$');

        return regex.toString();
    }
}