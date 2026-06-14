package com.tracemindai.archival.repository;

import com.tracemindai.archival.entity.Record;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface RecordRepository extends JpaRepository<Record, Long> {
    @Modifying
    @Transactional
    @Query(value = "UPDATE record SET status = :status WHERE record_id = :recordId", nativeQuery = true)
    int updateStatusByRecordId(String recordId, String status);
}
