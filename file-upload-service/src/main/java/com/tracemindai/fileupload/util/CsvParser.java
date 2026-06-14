package com.tracemindai.fileupload.util;

import com.tracemindai.fileupload.dto.MemberRecord;
import com.tracemindai.fileupload.exception.FileUploadException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class CsvParser {
    private static final String[] CSV_HEADERS = {
        "memberId", "name", "mobile", "email", "communicationPreference"
    };

    public List<MemberRecord> parse(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileUploadException("File is empty or null", "INVALID_FILE", 400);
        }

        if (!isValidCsvFile(file.getOriginalFilename())) {
            throw new FileUploadException("File must be a CSV file", "INVALID_FILE_TYPE", 400);
        }

        List<MemberRecord> records = new ArrayList<>();
        try (InputStreamReader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {

            int lineNumber = 1;
            for (CSVRecord csvRecord : csvParser) {
                lineNumber++;
                try {
                    MemberRecord record = parseLine(csvRecord, lineNumber);
                    records.add(record);
                } catch (IllegalArgumentException e) {
                    log.warn("Skipping invalid record at line {}: {}", lineNumber, e.getMessage());
                }
            }

            if (records.isEmpty()) {
                throw new FileUploadException("CSV file contains no valid records", "NO_RECORDS", 400);
            }

            log.info("Successfully parsed {} records from CSV file", records.size());
            return records;
        } catch (FileUploadException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error parsing CSV file", e);
            throw new FileUploadException("Error parsing CSV file: " + e.getMessage(), e, "CSV_PARSE_ERROR", 400);
        }
    }

    private MemberRecord parseLine(CSVRecord csvRecord, int lineNumber) {
        try {
            String memberId = csvRecord.get("memberId").trim();
            String name = csvRecord.get("name").trim();
            String mobile = csvRecord.get("mobile").trim();
            String email = csvRecord.get("email").trim();
            String communicationPreference = csvRecord.get("communicationPreference").trim();

            if (memberId.isEmpty() || name.isEmpty() || mobile.isEmpty() || email.isEmpty() || communicationPreference.isEmpty()) {
                throw new IllegalArgumentException("Required field is empty");
            }

            return MemberRecord.builder()
                .memberId(memberId)
                .name(name)
                .mobile(mobile)
                .email(email)
                .communicationPreference(communicationPreference)
                .build();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid field at line " + lineNumber + ": " + e.getMessage());
        }
    }

    private boolean isValidCsvFile(String filename) {
        return filename != null && filename.toLowerCase().endsWith(".csv");
    }
}
