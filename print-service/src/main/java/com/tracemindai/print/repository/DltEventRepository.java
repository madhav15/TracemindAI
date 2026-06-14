package com.tracemindai.print.repository;

import com.tracemindai.print.entity.DltEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DltEventRepository extends JpaRepository<DltEvent, Long> {
}
