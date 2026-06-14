package com.tracemindai.fileupload.controller;

import com.tracemindai.common.dto.ApiResponse;
import com.tracemindai.fileupload.dto.FileUploadResponse;
import com.tracemindai.fileupload.service.FileUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class FileUploadController {
    private final FileUploadService fileUploadService;

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadCsv(@RequestParam("file") MultipartFile file) {
        log.info("Received CSV upload request for file: {}", file.getOriginalFilename());

        FileUploadResponse response = fileUploadService.uploadCsv(file);

        ApiResponse<FileUploadResponse> apiResponse = ApiResponse.<FileUploadResponse>builder()
            .code(HttpStatus.CREATED.value())
            .message("File uploaded successfully")
            .data(response)
            .timestamp(LocalDateTime.now().toString())
            .build();

        log.info("CSV upload completed successfully for jobId: {}", response.getJobId());
        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
    }
}
