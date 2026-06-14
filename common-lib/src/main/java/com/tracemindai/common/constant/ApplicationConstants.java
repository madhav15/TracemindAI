package com.tracemindai.common.constant;

public final class ApplicationConstants {
    private ApplicationConstants() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    public static final String APPLICATION_NAME = "TracemindAI";
    public static final String API_VERSION = "v1";
    public static final String API_BASE_PATH = "/api/v1";

    public static final String CONTENT_TYPE_JSON = "application/json";
    public static final String CONTENT_TYPE_PDF = "application/pdf";
    public static final String CONTENT_TYPE_OCTET_STREAM = "application/octet-stream";

    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    public static final long DEFAULT_TIMEOUT_SECONDS = 30;
    public static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10 MB
}
