package com.tracemindai.archival.repository;

import com.tracemindai.archival.entity.DltEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DltEventRepository extends JpaRepository<DltEvent, Long> {
}
