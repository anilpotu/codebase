package com.enterprise.health.controller;

import com.enterprise.health.dto.CreateVitalRequest;
import com.enterprise.health.repository.VitalRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser
class VitalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VitalRepository vitalRepository;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        vitalRepository.deleteAll();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void recordVital_returns200() throws Exception {
        CreateVitalRequest request = new CreateVitalRequest();
        request.setUserId(1L);
        request.setHeartRate(72);
        request.setSystolicBp(120);
        request.setDiastolicBp(80);
        request.setTemperatureCelsius(36.6);
        request.setOxygenSaturation(98);

        mockMvc.perform(post("/api/vitals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", is(1)))
                .andExpect(jsonPath("$.heartRate", is(72)))
                .andExpect(jsonPath("$.systolicBp", is(120)))
                .andExpect(jsonPath("$.diastolicBp", is(80)))
                .andExpect(jsonPath("$.temperatureCelsius", is(36.6)))
                .andExpect(jsonPath("$.oxygenSaturation", is(98)))
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.recordedAt", notNullValue()));
    }

    @Test
    void getVitalsByUserId_returns200() throws Exception {
        // Record two vitals for the same user
        CreateVitalRequest request1 = new CreateVitalRequest();
        request1.setUserId(2L);
        request1.setHeartRate(70);
        request1.setSystolicBp(118);
        request1.setDiastolicBp(78);
        request1.setTemperatureCelsius(36.5);
        request1.setOxygenSaturation(99);

        CreateVitalRequest request2 = new CreateVitalRequest();
        request2.setUserId(2L);
        request2.setHeartRate(85);
        request2.setSystolicBp(130);
        request2.setDiastolicBp(88);
        request2.setTemperatureCelsius(37.2);
        request2.setOxygenSaturation(96);

        mockMvc.perform(post("/api/vitals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/vitals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isOk());

        // Fetch all vitals for user
        mockMvc.perform(get("/api/vitals/user/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].userId", is(2)))
                .andExpect(jsonPath("$[1].userId", is(2)));
    }

    @Test
    void getVitalsByUserId_emptyList() throws Exception {
        mockMvc.perform(get("/api/vitals/user/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getLatestVital_returns200() throws Exception {
        // Record two vitals
        CreateVitalRequest request1 = new CreateVitalRequest();
        request1.setUserId(3L);
        request1.setHeartRate(65);
        request1.setSystolicBp(115);
        request1.setDiastolicBp(75);
        request1.setTemperatureCelsius(36.4);
        request1.setOxygenSaturation(99);

        CreateVitalRequest request2 = new CreateVitalRequest();
        request2.setUserId(3L);
        request2.setHeartRate(90);
        request2.setSystolicBp(140);
        request2.setDiastolicBp(90);
        request2.setTemperatureCelsius(38.0);
        request2.setOxygenSaturation(95);

        mockMvc.perform(post("/api/vitals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/vitals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isOk());

        // Get latest vital - should be the second one recorded
        mockMvc.perform(get("/api/vitals/user/3/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", is(3)))
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.recordedAt", notNullValue()));
    }

    @Test
    void getLatestVital_notFound() throws Exception {
        mockMvc.perform(get("/api/vitals/user/999/latest"))
                .andExpect(status().isNotFound());
    }
}
