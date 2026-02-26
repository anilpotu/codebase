package com.enterprise.health.controller;

import com.enterprise.health.dto.CreateVitalRequest;
import com.enterprise.health.dto.VitalDTO;
import com.enterprise.health.service.HealthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/vitals")
public class VitalController {

    private static final Logger log = LoggerFactory.getLogger(VitalController.class);

    private final HealthService healthService;

    public VitalController(HealthService healthService) {
        this.healthService = healthService;
    }

    @PostMapping
    public ResponseEntity<VitalDTO> recordVital(@Valid @RequestBody CreateVitalRequest request) {
        log.info("POST /api/vitals - userId={}", request.getUserId());
        VitalDTO dto = healthService.recordVital(request);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<VitalDTO>> getVitalsByUserId(@PathVariable Long userId) {
        log.debug("GET /api/vitals/user/{}", userId);
        List<VitalDTO> vitals = healthService.getVitalsByUserId(userId);
        return ResponseEntity.ok(vitals);
    }

    @GetMapping("/user/{userId}/latest")
    public ResponseEntity<VitalDTO> getLatestVital(@PathVariable Long userId) {
        log.debug("GET /api/vitals/user/{}/latest", userId);
        return healthService.getLatestVital(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
