package com.tracemindai.archival.repository;

import com.tracemindai.archival.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {
    @Query(value = "SELECT COUNT(*) FROM record WHERE job_id = :jobId AND status != :status",
        nativeQuery = true)
    int countNonArchivedRecords(String jobId, String status);

    @Modifying
    @Transactional
    @Query(value = "UPDATE job SET status = :status WHERE job_id = :jobId", nativeQuery = true)
    int updateStatusByJobId(String jobId, String status);
}
