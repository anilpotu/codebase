package com.enterprise.health.service;

import com.enterprise.health.dto.CreateHealthRecordRequest;
import com.enterprise.health.dto.CreateVitalRequest;
import com.enterprise.health.dto.HealthRecordDTO;
import com.enterprise.health.dto.VitalDTO;
import com.enterprise.health.entity.HealthRecord;
import com.enterprise.health.entity.Vital;
import com.enterprise.health.repository.HealthRecordRepository;
import com.enterprise.health.repository.VitalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HealthServiceTest {

    @Mock
    private HealthRecordRepository healthRecordRepository;

    @Mock
    private VitalRepository vitalRepository;

    @InjectMocks
    private HealthService healthService;

    private CreateHealthRecordRequest healthRecordRequest;
    private HealthRecord existingRecord;
    private CreateVitalRequest vitalRequest;
    private Vital savedVital;

    @BeforeEach
    void setUp() {
        healthRecordRequest = new CreateHealthRecordRequest();
        healthRecordRequest.setUserId(1L);
        healthRecordRequest.setBloodType("A+");
        healthRecordRequest.setHeightCm(175.0);
        healthRecordRequest.setWeightKg(70.0);
        healthRecordRequest.setAllergies("Peanuts");
        healthRecordRequest.setConditions("None");
        healthRecordRequest.setMedications("Vitamin D");
        healthRecordRequest.setLastCheckupDate(LocalDate.of(2026, 1, 15));

        existingRecord = new HealthRecord();
        existingRecord.setId(1L);
        existingRecord.setUserId(1L);
        existingRecord.setBloodType("A+");
        existingRecord.setHeightCm(175.0);
        existingRecord.setWeightKg(70.0);
        existingRecord.setAllergies("Peanuts");
        existingRecord.setConditions("None");
        existingRecord.setMedications("Vitamin D");
        existingRecord.setLastCheckupDate(LocalDate.of(2026, 1, 15));
        existingRecord.setCreatedAt(LocalDateTime.of(2026, 1, 1, 10, 0));
        existingRecord.setUpdatedAt(LocalDateTime.of(2026, 1, 1, 10, 0));

        vitalRequest = new CreateVitalRequest();
        vitalRequest.setUserId(1L);
        vitalRequest.setHeartRate(72);
        vitalRequest.setSystolicBp(120);
        vitalRequest.setDiastolicBp(80);
        vitalRequest.setTemperatureCelsius(36.6);
        vitalRequest.setOxygenSaturation(98);

        savedVital = new Vital();
        savedVital.setId(1L);
        savedVital.setUserId(1L);
        savedVital.setHeartRate(72);
        savedVital.setSystolicBp(120);
        savedVital.setDiastolicBp(80);
        savedVital.setTemperatureCelsius(36.6);
        savedVital.setOxygenSaturation(98);
        savedVital.setRecordedAt(LocalDateTime.of(2026, 2, 17, 12, 0));
    }

    @Test
    void createHealthRecord_new() {
        when(healthRecordRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(healthRecordRepository.save(any(HealthRecord.class))).thenReturn(existingRecord);

        HealthRecordDTO result = healthService.createOrUpdateHealthRecord(healthRecordRequest);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(1L, result.getUserId());
        assertEquals("A+", result.getBloodType());
        assertEquals(175.0, result.getHeightCm());
        assertEquals(70.0, result.getWeightKg());
        assertEquals("Peanuts", result.getAllergies());
        assertEquals("None", result.getConditions());
        assertEquals("Vitamin D", result.getMedications());
        assertEquals(LocalDate.of(2026, 1, 15), result.getLastCheckupDate());

        verify(healthRecordRepository).findByUserId(1L);
        verify(healthRecordRepository).save(any(HealthRecord.class));
    }

    @Test
    void updateHealthRecord_existing() {
        when(healthRecordRepository.findByUserId(1L)).thenReturn(Optional.of(existingRecord));

        HealthRecord updatedRecord = new HealthRecord();
        updatedRecord.setId(1L);
        updatedRecord.setUserId(1L);
        updatedRecord.setBloodType("B+");
        updatedRecord.setHeightCm(176.0);
        updatedRecord.setWeightKg(72.0);
        updatedRecord.setAllergies("None");
        updatedRecord.setConditions("None");
        updatedRecord.setMedications("None");
        updatedRecord.setLastCheckupDate(LocalDate.of(2026, 2, 10));
        updatedRecord.setCreatedAt(LocalDateTime.of(2026, 1, 1, 10, 0));
        updatedRecord.setUpdatedAt(LocalDateTime.of(2026, 2, 10, 10, 0));

        when(healthRecordRepository.save(any(HealthRecord.class))).thenReturn(updatedRecord);

        CreateHealthRecordRequest updateRequest = new CreateHealthRecordRequest();
        updateRequest.setUserId(1L);
        updateRequest.setBloodType("B+");
        updateRequest.setHeightCm(176.0);
        updateRequest.setWeightKg(72.0);
        updateRequest.setAllergies("None");
        updateRequest.setConditions("None");
        updateRequest.setMedications("None");
        updateRequest.setLastCheckupDate(LocalDate.of(2026, 2, 10));

        HealthRecordDTO result = healthService.createOrUpdateHealthRecord(updateRequest);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("B+", result.getBloodType());
        assertEquals(176.0, result.getHeightCm());
        assertEquals(72.0, result.getWeightKg());

        verify(healthRecordRepository).findByUserId(1L);
        verify(healthRecordRepository).save(existingRecord);
    }

    @Test
    void getHealthRecordByUserId_found() {
        when(healthRecordRepository.findByUserId(1L)).thenReturn(Optional.of(existingRecord));

        Optional<HealthRecordDTO> result = healthService.getHealthRecordByUserId(1L);

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getUserId());
        assertEquals("A+", result.get().getBloodType());
        verify(healthRecordRepository).findByUserId(1L);
    }

    @Test
    void getHealthRecordByUserId_notFound() {
        when(healthRecordRepository.findByUserId(999L)).thenReturn(Optional.empty());

        Optional<HealthRecordDTO> result = healthService.getHealthRecordByUserId(999L);

        assertFalse(result.isPresent());
        verify(healthRecordRepository).findByUserId(999L);
    }

    @Test
    void recordVital_success() {
        when(vitalRepository.save(any(Vital.class))).thenReturn(savedVital);

        VitalDTO result = healthService.recordVital(vitalRequest);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(1L, result.getUserId());
        assertEquals(72, result.getHeartRate());
        assertEquals(120, result.getSystolicBp());
        assertEquals(80, result.getDiastolicBp());
        assertEquals(36.6, result.getTemperatureCelsius());
        assertEquals(98, result.getOxygenSaturation());
        assertNotNull(result.getRecordedAt());

        verify(vitalRepository).save(any(Vital.class));
    }

    @Test
    void getVitalsByUserId_returnsList() {
        Vital secondVital = new Vital();
        secondVital.setId(2L);
        secondVital.setUserId(1L);
        secondVital.setHeartRate(80);
        secondVital.setSystolicBp(125);
        secondVital.setDiastolicBp(85);
        secondVital.setTemperatureCelsius(37.0);
        secondVital.setOxygenSaturation(97);
        secondVital.setRecordedAt(LocalDateTime.of(2026, 2, 16, 12, 0));

        when(vitalRepository.findByUserIdOrderByRecordedAtDesc(1L))
                .thenReturn(Arrays.asList(savedVital, secondVital));

        List<VitalDTO> result = healthService.getVitalsByUserId(1L);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(72, result.get(0).getHeartRate());
        assertEquals(80, result.get(1).getHeartRate());
        verify(vitalRepository).findByUserIdOrderByRecordedAtDesc(1L);
    }

    @Test
    void getVitalsByUserId_emptyList() {
        when(vitalRepository.findByUserIdOrderByRecordedAtDesc(999L))
                .thenReturn(Collections.emptyList());

        List<VitalDTO> result = healthService.getVitalsByUserId(999L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(vitalRepository).findByUserIdOrderByRecordedAtDesc(999L);
    }

    @Test
    void getLatestVital_found() {
        when(vitalRepository.findTopByUserIdOrderByRecordedAtDesc(1L))
                .thenReturn(Optional.of(savedVital));

        Optional<VitalDTO> result = healthService.getLatestVital(1L);

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
        assertEquals(72, result.get().getHeartRate());
        verify(vitalRepository).findTopByUserIdOrderByRecordedAtDesc(1L);
    }

    @Test
    void getLatestVital_notFound() {
        when(vitalRepository.findTopByUserIdOrderByRecordedAtDesc(999L))
                .thenReturn(Optional.empty());

        Optional<VitalDTO> result = healthService.getLatestVital(999L);

        assertFalse(result.isPresent());
        verify(vitalRepository).findTopByUserIdOrderByRecordedAtDesc(999L);
    }
}
