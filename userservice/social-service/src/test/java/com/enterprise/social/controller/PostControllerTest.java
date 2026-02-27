package com.enterprise.social.controller;

import com.enterprise.social.client.UserGrpcServiceClient;
import com.enterprise.social.dto.GrpcUserDTO;
import com.enterprise.social.entity.Post;
import com.enterprise.social.repository.PostRepository;
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
class PostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PostRepository postRepository;

    @MockBean
    private UserGrpcServiceClient userGrpcServiceClient;

    @BeforeEach
    void setUp() {
        postRepository.deleteAll();
    }

    @Test
    void createPost_returns200() throws Exception {
        GrpcUserDTO user = new GrpcUserDTO();
        user.setId(1L);
        user.setName("Test User");
        when(userGrpcServiceClient.getUserById(1L)).thenReturn(ResponseEntity.ok(user));

        String json = "{" +
                "\"userId\": 1," +
                "\"content\": \"My first post\"" +
                "}";

        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.content").value("My first post"))
                .andExpect(jsonPath("$.likesCount").value(0))
                .andExpect(jsonPath("$.commentsCount").value(0));
    }

    @Test
    void getPostsByUserId_returns200() throws Exception {
        Post post1 = new Post();
        post1.setUserId(1L);
        post1.setContent("First post");
        postRepository.save(post1);

        Post post2 = new Post();
        post2.setUserId(1L);
        post2.setContent("Second post");
        postRepository.save(post2);

        mockMvc.perform(get("/api/posts/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }
}
