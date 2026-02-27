package com.enterprise.social.controller;

import com.enterprise.social.client.UserGrpcServiceClient;
import com.enterprise.social.dto.GrpcUserDTO;
import com.enterprise.social.entity.SocialProfile;
import com.enterprise.social.repository.SocialProfileRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SocialProfileRepository profileRepository;

    @MockBean
    private UserGrpcServiceClient userGrpcServiceClient;

    @BeforeEach
    void setUp() {
        profileRepository.deleteAll();
    }

    @Test
    void createProfile_returns200() throws Exception {
        GrpcUserDTO user = new GrpcUserDTO();
        user.setId(1L);
        user.setName("Test User");
        when(userGrpcServiceClient.getUserById(1L)).thenReturn(ResponseEntity.ok(user));

        String json = "{" +
                "\"userId\": 1," +
                "\"displayName\": \"John Doe\"," +
                "\"bio\": \"Hello world\"," +
                "\"location\": \"NYC\"," +
                "\"website\": \"https://example.com\"" +
                "}";

        mockMvc.perform(post("/api/profiles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.displayName").value("John Doe"))
                .andExpect(jsonPath("$.bio").value("Hello world"))
                .andExpect(jsonPath("$.location").value("NYC"))
                .andExpect(jsonPath("$.website").value("https://example.com"))
                .andExpect(jsonPath("$.followersCount").value(0))
                .andExpect(jsonPath("$.followingCount").value(0));
    }

    @Test
    void getProfileByUserId_returns200() throws Exception {
        SocialProfile profile = new SocialProfile();
        profile.setUserId(1L);
        profile.setDisplayName("John Doe");
        profile.setBio("Hello world");
        profile.setLocation("NYC");
        profile.setWebsite("https://example.com");
        profileRepository.save(profile);

        mockMvc.perform(get("/api/profiles/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.displayName").value("John Doe"))
                .andExpect(jsonPath("$.bio").value("Hello world"));
    }
}
