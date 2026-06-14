package com.tracemindai.fileupload.util;

import com.tracemindai.fileupload.config.Snowflake;

public class IdGenerator {

    private static final Snowflake sf = new Snowflake(275);

    public static String generateSnowflakeId() {
        return String.valueOf(sf.nextId());
    }
}
