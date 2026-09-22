package de.deringo.forgemind.core.util;

/**
 * Utility class for text operations.
 */
public final class TextUtils {
    
    /**
     * Private constructor to prevent instantiation.
     */
    private TextUtils() {
        // Prevent instantiation
    }
    
    /**
     * Checks if a string is blank (null, empty, or contains only whitespace).
     *
     * @param value the string to check
     * @return true if the string is null, empty, or contains only whitespace; false otherwise
     */
    public static boolean isBlank(String value) {
        return value == null || value.equals("") || value.trim().isEmpty();
    }
}