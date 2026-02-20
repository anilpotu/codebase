package com.enterprise.social.controller;

import com.enterprise.social.entity.Connection;
import com.enterprise.social.repository.ConnectionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser
class ConnectionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ConnectionRepository connectionRepository;

    @BeforeEach
    void setUp() {
        connectionRepository.deleteAll();
    }

    @Test
    void sendConnectionRequest_returns200() throws Exception {
        String json = "{" +
                "\"userId\": 1," +
                "\"connectedUserId\": 2" +
                "}";

        mockMvc.perform(post("/api/connections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.connectedUserId").value(2))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void acceptConnection_returns200() throws Exception {
        Connection connection = new Connection();
        connection.setUserId(1L);
        connection.setConnectedUserId(2L);
        connection.setStatus("PENDING");
        Connection saved = connectionRepository.save(connection);

        mockMvc.perform(put("/api/connections/" + saved.getId() + "/accept"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    void getConnections_returns200() throws Exception {
        Connection connection = new Connection();
        connection.setUserId(1L);
        connection.setConnectedUserId(2L);
        connection.setStatus("ACCEPTED");
        connectionRepository.save(connection);

        mockMvc.perform(get("/api/connections/user/1")
                        .param("status", "ACCEPTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userId").value(1))
                .andExpect(jsonPath("$[0].connectedUserId").value(2))
                .andExpect(jsonPath("$[0].status").value("ACCEPTED"));
    }
}
