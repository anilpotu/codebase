package com.enterprise.social.service;

import com.enterprise.social.dto.*;
import com.enterprise.social.entity.Connection;
import com.enterprise.social.entity.Post;
import com.enterprise.social.entity.SocialProfile;
import com.enterprise.social.repository.ConnectionRepository;
import com.enterprise.social.repository.PostRepository;
import com.enterprise.social.repository.SocialProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SocialServiceTest {

    @Mock
    private SocialProfileRepository profileRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private ConnectionRepository connectionRepository;

    @InjectMocks
    private SocialService socialService;

    // --- Profile Tests ---

    @Test
    void createProfile_new_savesAndReturnsDTO() {
        CreateProfileRequest request = new CreateProfileRequest();
        request.setUserId(1L);
        request.setDisplayName("John Doe");
        request.setBio("Hello world");
        request.setLocation("NYC");
        request.setWebsite("https://example.com");

        when(profileRepository.findByUserId(1L)).thenReturn(Optional.empty());

        SocialProfile saved = buildProfile(10L, 1L, "John Doe", "Hello world", "NYC", "https://example.com");
        when(profileRepository.save(any(SocialProfile.class))).thenReturn(saved);

        SocialProfileDTO result = socialService.createOrUpdateProfile(request);

        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals(1L, result.getUserId());
        assertEquals("John Doe", result.getDisplayName());
        assertEquals("Hello world", result.getBio());
        assertEquals("NYC", result.getLocation());
        assertEquals("https://example.com", result.getWebsite());
        verify(profileRepository).findByUserId(1L);
        verify(profileRepository).save(any(SocialProfile.class));
    }

    @Test
    void updateProfile_existing_updatesAndReturnsDTO() {
        SocialProfile existing = buildProfile(10L, 1L, "Old Name", "Old bio", "LA", "https://old.com");

        CreateProfileRequest request = new CreateProfileRequest();
        request.setUserId(1L);
        request.setDisplayName("New Name");
        request.setBio("New bio");
        request.setLocation("SF");
        request.setWebsite("https://new.com");

        when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(existing));

        SocialProfile updated = buildProfile(10L, 1L, "New Name", "New bio", "SF", "https://new.com");
        when(profileRepository.save(any(SocialProfile.class))).thenReturn(updated);

        SocialProfileDTO result = socialService.createOrUpdateProfile(request);

        assertNotNull(result);
        assertEquals("New Name", result.getDisplayName());
        assertEquals("New bio", result.getBio());
        assertEquals("SF", result.getLocation());
        verify(profileRepository).findByUserId(1L);
        verify(profileRepository).save(existing);
    }

    @Test
    void getProfileByUserId_found_returnsDTO() {
        SocialProfile profile = buildProfile(10L, 1L, "John", "Bio", "NYC", "https://example.com");
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));

        SocialProfileDTO result = socialService.getProfileByUserId(1L);

        assertNotNull(result);
        assertEquals(1L, result.getUserId());
        assertEquals("John", result.getDisplayName());
    }

    @Test
    void getProfileByUserId_notFound_throwsException() {
        when(profileRepository.findByUserId(999L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> socialService.getProfileByUserId(999L));
        assertTrue(ex.getMessage().contains("Profile not found"));
    }

    // --- Post Tests ---

    @Test
    void createPost_success_savesAndReturnsDTO() {
        CreatePostRequest request = new CreatePostRequest();
        request.setUserId(1L);
        request.setContent("My first post");

        Post saved = buildPost(20L, 1L, "My first post");
        when(postRepository.save(any(Post.class))).thenReturn(saved);

        PostDTO result = socialService.createPost(request);

        assertNotNull(result);
        assertEquals(20L, result.getId());
        assertEquals(1L, result.getUserId());
        assertEquals("My first post", result.getContent());
        assertEquals(0, result.getLikesCount());
        assertEquals(0, result.getCommentsCount());
        verify(postRepository).save(any(Post.class));
    }

    @Test
    void getPostsByUserId_returnsList() {
        Post post1 = buildPost(1L, 1L, "Post 1");
        Post post2 = buildPost(2L, 1L, "Post 2");
        when(postRepository.findByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(Arrays.asList(post1, post2));

        List<PostDTO> result = socialService.getPostsByUserId(1L);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Post 1", result.get(0).getContent());
        assertEquals("Post 2", result.get(1).getContent());
    }

    // --- Connection Tests ---

    @Test
    void sendConnectionRequest_success_savesWithPendingStatus() {
        CreateConnectionRequest request = new CreateConnectionRequest();
        request.setUserId(1L);
        request.setConnectedUserId(2L);

        Connection saved = buildConnection(30L, 1L, 2L, "PENDING");
        when(connectionRepository.save(any(Connection.class))).thenReturn(saved);

        ConnectionDTO result = socialService.sendConnectionRequest(request);

        assertNotNull(result);
        assertEquals(30L, result.getId());
        assertEquals(1L, result.getUserId());
        assertEquals(2L, result.getConnectedUserId());
        assertEquals("PENDING", result.getStatus());
        verify(connectionRepository).save(any(Connection.class));
    }

    @Test
    void acceptConnection_success_setsStatusToAccepted() {
        Connection existing = buildConnection(30L, 1L, 2L, "PENDING");
        when(connectionRepository.findById(30L)).thenReturn(Optional.of(existing));

        Connection accepted = buildConnection(30L, 1L, 2L, "ACCEPTED");
        when(connectionRepository.save(any(Connection.class))).thenReturn(accepted);

        when(profileRepository.findByUserId(2L)).thenReturn(Optional.empty());
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.empty());

        ConnectionDTO result = socialService.acceptConnection(30L);

        assertNotNull(result);
        assertEquals("ACCEPTED", result.getStatus());
        verify(connectionRepository).findById(30L);
        verify(connectionRepository).save(any(Connection.class));
    }

    @Test
    void getConnections_withStatus_returnsList() {
        Connection conn = buildConnection(30L, 1L, 2L, "ACCEPTED");
        when(connectionRepository.findByUserIdAndStatus(1L, "ACCEPTED"))
                .thenReturn(Collections.singletonList(conn));

        List<ConnectionDTO> result = socialService.getConnections(1L, "ACCEPTED");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("ACCEPTED", result.get(0).getStatus());
    }

    @Test
    void getConnections_withoutStatus_defaultsToAccepted() {
        Connection conn = buildConnection(30L, 1L, 2L, "ACCEPTED");
        when(connectionRepository.findByUserIdAndStatus(1L, "ACCEPTED"))
                .thenReturn(Collections.singletonList(conn));

        List<ConnectionDTO> result = socialService.getConnections(1L, null);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(connectionRepository).findByUserIdAndStatus(1L, "ACCEPTED");
    }

    // --- Helper methods ---

    private SocialProfile buildProfile(Long id, Long userId, String displayName,
                                       String bio, String location, String website) {
        SocialProfile profile = new SocialProfile();
        profile.setId(id);
        profile.setUserId(userId);
        profile.setDisplayName(displayName);
        profile.setBio(bio);
        profile.setLocation(location);
        profile.setWebsite(website);
        profile.setFollowersCount(0);
        profile.setFollowingCount(0);
        profile.setCreatedAt(LocalDateTime.now());
        profile.setUpdatedAt(LocalDateTime.now());
        return profile;
    }

    private Post buildPost(Long id, Long userId, String content) {
        Post post = new Post();
        post.setId(id);
        post.setUserId(userId);
        post.setContent(content);
        post.setLikesCount(0);
        post.setCommentsCount(0);
        post.setCreatedAt(LocalDateTime.now());
        return post;
    }

    private Connection buildConnection(Long id, Long userId, Long connectedUserId, String status) {
        Connection connection = new Connection();
        connection.setId(id);
        connection.setUserId(userId);
        connection.setConnectedUserId(connectedUserId);
        connection.setStatus(status);
        connection.setCreatedAt(LocalDateTime.now());
        return connection;
    }
}
