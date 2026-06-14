package com.tracemindai.print.repository;

import com.tracemindai.print.entity.PrintJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PrintJobRepository extends JpaRepository<PrintJob, Long> {
    Optional<PrintJob> findByRecordId(String recordId);

    List<PrintJob> findByJobId(String jobId);

    List<PrintJob> findByStatus(String status);

    List<PrintJob> findByJobIdAndStatus(String jobId, String status);
}
