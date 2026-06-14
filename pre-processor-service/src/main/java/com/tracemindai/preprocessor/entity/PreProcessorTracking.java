package com.tracemindai.preprocessor.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "pre_processor_tracking")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreProcessorTracking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "job_id", nullable = false)
    private String jobId;

    @Column(name = "record_id", nullable = false)
    private String recordId;

    @Column(name = "member_id", nullable = false)
    private String memberId;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "processing_type", length = 100)
    private String processingType;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
