package com.enterprise.health.dto;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;

public class CreateHealthRecordRequest {

    @NotNull
    private Long userId;

    private String bloodType;

    private Double heightCm;

    private Double weightKg;

    private String allergies;

    private String conditions;

    private String medications;

    private LocalDate lastCheckupDate;

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
}
