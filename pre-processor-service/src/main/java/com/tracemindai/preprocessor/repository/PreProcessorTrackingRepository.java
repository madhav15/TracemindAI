package com.tracemindai.preprocessor.repository;

import com.tracemindai.preprocessor.entity.PreProcessorTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PreProcessorTrackingRepository extends JpaRepository<PreProcessorTracking, Long> {
    Optional<PreProcessorTracking> findByRecordId(String recordId);

    List<PreProcessorTracking> findByJobId(String jobId);

    List<PreProcessorTracking> findByStatus(String status);

    List<PreProcessorTracking> findByJobIdAndStatus(String jobId, String status);
}
