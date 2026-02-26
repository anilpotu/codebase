package com.enterprise.health.repository;

import com.enterprise.health.entity.HealthRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HealthRecordRepository extends JpaRepository<HealthRecord, Long> {

    Optional<HealthRecord> findByUserId(Long userId);
}
