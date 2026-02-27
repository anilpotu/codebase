package com.enterprise.health.controller;

import com.enterprise.health.client.UserGrpcServiceClient;
import com.enterprise.health.dto.CreateHealthRecordRequest;
import com.enterprise.health.dto.GrpcUserDTO;
import com.enterprise.health.entity.HealthRecord;
import com.enterprise.health.repository.HealthRecordRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser
class HealthRecordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HealthRecordRepository healthRecordRepository;

    @MockBean
    private UserGrpcServiceClient userGrpcServiceClient;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        healthRecordRepository.deleteAll();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        GrpcUserDTO user = new GrpcUserDTO();
        user.setName("Test User");
        when(userGrpcServiceClient.getUserById(anyLong())).thenAnswer(invocation -> {
            user.setId(invocation.getArgument(0));
            return ResponseEntity.ok(user);
        });
    }

    @Test
    void createHealthRecord_returns200() throws Exception {
        CreateHealthRecordRequest request = new CreateHealthRecordRequest();
        request.setUserId(1L);
        request.setBloodType("O+");
        request.setHeightCm(180.0);
        request.setWeightKg(75.0);
        request.setAllergies("Dust");
        request.setConditions("Asthma");
        request.setMedications("Inhaler");
        request.setLastCheckupDate(LocalDate.of(2026, 1, 20));

        mockMvc.perform(post("/api/health-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", is(1)))
                .andExpect(jsonPath("$.bloodType", is("O+")))
                .andExpect(jsonPath("$.heightCm", is(180.0)))
                .andExpect(jsonPath("$.weightKg", is(75.0)))
                .andExpect(jsonPath("$.allergies", is("Dust")))
                .andExpect(jsonPath("$.conditions", is("Asthma")))
                .andExpect(jsonPath("$.medications", is("Inhaler")))
                .andExpect(jsonPath("$.id", notNullValue()));
    }

    @Test
    void createHealthRecord_updatesExisting() throws Exception {
        CreateHealthRecordRequest request = new CreateHealthRecordRequest();
        request.setUserId(2L);
        request.setBloodType("A-");
        request.setHeightCm(165.0);
        request.setWeightKg(60.0);

        // Create initial record
        mockMvc.perform(post("/api/health-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Update the same user's record
        request.setBloodType("A+");
        request.setWeightKg(62.0);

        mockMvc.perform(post("/api/health-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", is(2)))
                .andExpect(jsonPath("$.bloodType", is("A+")))
                .andExpect(jsonPath("$.weightKg", is(62.0)));

        // Verify only one record exists for this user
        assertEquals(1L, healthRecordRepository.findAll().stream()
                .filter(r -> r.getUserId().equals(2L))
                .count());
    }

    @Test
    void getHealthRecordByUserId_returns200() throws Exception {
        // First create a record
        CreateHealthRecordRequest request = new CreateHealthRecordRequest();
        request.setUserId(3L);
        request.setBloodType("B+");
        request.setHeightCm(170.0);
        request.setWeightKg(68.0);
        request.setAllergies("None");

        mockMvc.perform(post("/api/health-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Then fetch it
        mockMvc.perform(get("/api/health-records/user/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", is(3)))
                .andExpect(jsonPath("$.bloodType", is("B+")))
                .andExpect(jsonPath("$.heightCm", is(170.0)))
                .andExpect(jsonPath("$.weightKg", is(68.0)))
                .andExpect(jsonPath("$.allergies", is("None")));
    }

    @Test
    void getHealthRecordByUserId_notFound() throws Exception {
        mockMvc.perform(get("/api/health-records/user/999"))
                .andExpect(status().isNotFound());
    }
}
