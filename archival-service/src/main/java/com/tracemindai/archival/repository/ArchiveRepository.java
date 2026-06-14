package com.tracemindai.archival.repository;

import com.tracemindai.archival.entity.Archive;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArchiveRepository extends JpaRepository<Archive, Long> {
    Optional<Archive> findByRecordId(String recordId);

    List<Archive> findByJobId(String jobId);

    List<Archive> findByStatus(String status);

    List<Archive> findByJobIdAndStatus(String jobId, String status);
}
