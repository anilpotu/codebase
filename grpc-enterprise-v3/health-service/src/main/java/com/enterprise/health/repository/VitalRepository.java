package com.enterprise.health.repository;

import com.enterprise.health.entity.Vital;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VitalRepository extends JpaRepository<Vital, Long> {

    List<Vital> findByUserIdOrderByRecordedAtDesc(Long userId);

    Optional<Vital> findTopByUserIdOrderByRecordedAtDesc(Long userId);
}
