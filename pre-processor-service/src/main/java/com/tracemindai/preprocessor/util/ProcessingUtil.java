package com.tracemindai.preprocessor.util;

public final class ProcessingUtil {
    private ProcessingUtil() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    public static boolean isValidFileId(String fileId) {
        return fileId != null && !fileId.trim().isEmpty();
    }

    public static String generateProcessingJobId() {
        return "JOB_" + System.currentTimeMillis();
    }
}
