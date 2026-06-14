package com.tracemindai.archival.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "record")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Record {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "record_id", nullable = false, unique = true)
    private String recordId;

    @Column(name = "job_id", nullable = false)
    private String jobId;

    @Column(name = "member_id", nullable = false)
    private String memberId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "mobile", nullable = false)
    private String mobile;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "communication_preference", nullable = false)
    private String communicationPreference;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
