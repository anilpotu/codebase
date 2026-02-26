package com.enterprise.health.controller;

import com.enterprise.health.dto.CreateHealthRecordRequest;
import com.enterprise.health.dto.HealthRecordDTO;
import com.enterprise.health.service.HealthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/health-records")
public class HealthRecordController {

    private static final Logger log = LoggerFactory.getLogger(HealthRecordController.class);

    private final HealthService healthService;

    public HealthRecordController(HealthService healthService) {
        this.healthService = healthService;
    }

    @PostMapping
    public ResponseEntity<HealthRecordDTO> createOrUpdate(@Valid @RequestBody CreateHealthRecordRequest request) {
        log.info("POST /api/health-records - userId={}", request.getUserId());
        HealthRecordDTO dto = healthService.createOrUpdateHealthRecord(request);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<HealthRecordDTO> getByUserId(@PathVariable Long userId) {
        log.debug("GET /api/health-records/user/{}", userId);
        return healthService.getHealthRecordByUserId(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
