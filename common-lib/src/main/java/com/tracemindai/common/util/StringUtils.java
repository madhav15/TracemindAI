package com.tracemindai.common.util;

public final class StringUtils {
    private StringUtils() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static boolean isNotBlank(String value) {
        return !isBlank(value);
    }

    public static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    public static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
