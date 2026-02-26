package com.enterprise.health.service;

import com.enterprise.health.client.UserGrpcServiceClient;
import com.enterprise.health.dto.*;
import com.enterprise.health.entity.HealthRecord;
import com.enterprise.health.entity.Vital;
import com.enterprise.health.repository.HealthRecordRepository;
import com.enterprise.health.repository.VitalRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class HealthService {

    private static final Logger log = LoggerFactory.getLogger(HealthService.class);

    private final HealthRecordRepository healthRecordRepository;
    private final VitalRepository vitalRepository;
    private final UserGrpcServiceClient userGrpcServiceClient;

    public HealthService(HealthRecordRepository healthRecordRepository,
                         VitalRepository vitalRepository,
                         UserGrpcServiceClient userGrpcServiceClient) {
        this.healthRecordRepository = healthRecordRepository;
        this.vitalRepository = vitalRepository;
        this.userGrpcServiceClient = userGrpcServiceClient;
    }

    @Transactional
    @CircuitBreaker(name = "healthService")
    public HealthRecordDTO createOrUpdateHealthRecord(CreateHealthRecordRequest request) {
        log.info("Creating/updating health record for userId={}", request.getUserId());

        // Validate user exists in user-grpc-service before persisting health data
        try {
            ResponseEntity<GrpcUserDTO> userResponse = userGrpcServiceClient.getUserById(request.getUserId());
            if (!userResponse.getStatusCode().is2xxSuccessful() || userResponse.getBody() == null) {
                throw new RuntimeException("User not found with id: " + request.getUserId());
            }
            log.info("User validated for health record: userId={}, name={}",
                    request.getUserId(), userResponse.getBody().getName());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to validate user userId={}: {}", request.getUserId(), e.getMessage());
            throw new RuntimeException("User validation failed: " + e.getMessage());
        }
        HealthRecord record = healthRecordRepository.findByUserId(request.getUserId())
                .orElse(new HealthRecord());

        record.setUserId(request.getUserId());
        record.setBloodType(request.getBloodType());
        record.setHeightCm(request.getHeightCm());
        record.setWeightKg(request.getWeightKg());
        record.setAllergies(request.getAllergies());
        record.setConditions(request.getConditions());
        record.setMedications(request.getMedications());
        record.setLastCheckupDate(request.getLastCheckupDate());

        HealthRecord saved = healthRecordRepository.save(record);
        log.info("Health record saved: id={}, userId={}", saved.getId(), saved.getUserId());
        return toHealthRecordDTO(saved);
    }

    @Transactional(readOnly = true)
    @CircuitBreaker(name = "healthService")
    public Optional<HealthRecordDTO> getHealthRecordByUserId(Long userId) {
        log.debug("Fetching health record for userId={}", userId);
        return healthRecordRepository.findByUserId(userId)
                .map(this::toHealthRecordDTO);
    }

    @Transactional
    @CircuitBreaker(name = "healthService")
    public VitalDTO recordVital(CreateVitalRequest request) {
        log.info("Recording vital for userId={}", request.getUserId());
        Vital vital = new Vital();
        vital.setUserId(request.getUserId());
        vital.setHeartRate(request.getHeartRate());
        vital.setSystolicBp(request.getSystolicBp());
        vital.setDiastolicBp(request.getDiastolicBp());
        vital.setTemperatureCelsius(request.getTemperatureCelsius());
        vital.setOxygenSaturation(request.getOxygenSaturation());
        vital.setRecordedAt(LocalDateTime.now());

        Vital saved = vitalRepository.save(vital);
        log.info("Vital recorded: id={}, userId={}", saved.getId(), saved.getUserId());
        return toVitalDTO(saved);
    }

    @Transactional(readOnly = true)
    @CircuitBreaker(name = "healthService")
    public List<VitalDTO> getVitalsByUserId(Long userId) {
        log.debug("Fetching vitals for userId={}", userId);
        return vitalRepository.findByUserIdOrderByRecordedAtDesc(userId)
                .stream()
                .map(this::toVitalDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @CircuitBreaker(name = "healthService")
    public Optional<VitalDTO> getLatestVital(Long userId) {
        log.debug("Fetching latest vital for userId={}", userId);
        return vitalRepository.findTopByUserIdOrderByRecordedAtDesc(userId)
                .map(this::toVitalDTO);
    }

    private HealthRecordDTO toHealthRecordDTO(HealthRecord record) {
        return new HealthRecordDTO(
                record.getId(),
                record.getUserId(),
                record.getBloodType(),
                record.getHeightCm(),
                record.getWeightKg(),
                record.getAllergies(),
                record.getConditions(),
                record.getMedications(),
                record.getLastCheckupDate(),
                record.getCreatedAt(),
                record.getUpdatedAt()
        );
    }

    private VitalDTO toVitalDTO(Vital vital) {
        VitalDTO dto = new VitalDTO();
        dto.setId(vital.getId());
        dto.setUserId(vital.getUserId());
        dto.setHeartRate(vital.getHeartRate());
        dto.setSystolicBp(vital.getSystolicBp());
        dto.setDiastolicBp(vital.getDiastolicBp());
        dto.setTemperatureCelsius(vital.getTemperatureCelsius());
        dto.setOxygenSaturation(vital.getOxygenSaturation());
        dto.setRecordedAt(vital.getRecordedAt());
        return dto;
    }
}
