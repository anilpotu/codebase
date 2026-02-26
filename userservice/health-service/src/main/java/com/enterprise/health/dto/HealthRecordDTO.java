package com.enterprise.health.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class HealthRecordDTO {

    private Long id;
    private Long userId;
    private String bloodType;
    private Double heightCm;
    private Double weightKg;
    private String allergies;
    private String conditions;
    private String medications;
    private LocalDate lastCheckupDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public HealthRecordDTO() {
    }

    public HealthRecordDTO(Long id, Long userId, String bloodType, Double heightCm, Double weightKg,
                           String allergies, String conditions, String medications,
                           LocalDate lastCheckupDate, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.bloodType = bloodType;
        this.heightCm = heightCm;
        this.weightKg = weightKg;
        this.allergies = allergies;
        this.conditions = conditions;
        this.medications = medications;
        this.lastCheckupDate = lastCheckupDate;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getBloodType() {
        return bloodType;
    }

    public void setBloodType(String bloodType) {
        this.bloodType = bloodType;
    }

    public Double getHeightCm() {
        return heightCm;
    }

    public void setHeightCm(Double heightCm) {
        this.heightCm = heightCm;
    }

    public Double getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(Double weightKg) {
        this.weightKg = weightKg;
    }

    public String getAllergies() {
        return allergies;
    }

    public void setAllergies(String allergies) {
        this.allergies = allergies;
    }

    public String getConditions() {
        return conditions;
    }

    public void setConditions(String conditions) {
        this.conditions = conditions;
    }

    public String getMedications() {
        return medications;
    }

    public void setMedications(String medications) {
        this.medications = medications;
    }

    public LocalDate getLastCheckupDate() {
        return lastCheckupDate;
    }

    public void setLastCheckupDate(LocalDate lastCheckupDate) {
        this.lastCheckupDate = lastCheckupDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
