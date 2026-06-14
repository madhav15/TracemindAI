package com.tracemindai.email.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_tracking")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailTracking {
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

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
