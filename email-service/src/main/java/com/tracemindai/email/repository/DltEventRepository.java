package com.tracemindai.email.repository;

import com.tracemindai.email.entity.DltEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DltEventRepository extends JpaRepository<DltEvent, Long> {
}
