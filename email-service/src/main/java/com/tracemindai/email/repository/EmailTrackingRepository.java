package com.tracemindai.email.repository;

import com.tracemindai.email.entity.EmailTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmailTrackingRepository extends JpaRepository<EmailTracking, Long> {
    Optional<EmailTracking> findByRecordId(String recordId);

    List<EmailTracking> findByJobId(String jobId);

    List<EmailTracking> findByStatus(String status);

    List<EmailTracking> findByJobIdAndStatus(String jobId, String status);
}
