package com.tracemindai.fileupload.util;

public final class FileValidationUtil {
    private FileValidationUtil() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    private static final String CSV_EXTENSION = "csv";

    public static boolean isValidFileName(String fileName) {
        return fileName != null && !fileName.trim().isEmpty();
    }

    public static String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }

    public static boolean isCsvFile(String fileName) {
        return isValidFileName(fileName) && getFileExtension(fileName).equals(CSV_EXTENSION);
    }
}
