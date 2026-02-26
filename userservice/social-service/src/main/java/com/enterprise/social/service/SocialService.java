package com.enterprise.social.service;

import com.enterprise.social.client.UserGrpcServiceClient;
import com.enterprise.social.dto.*;
import com.enterprise.social.entity.Connection;
import com.enterprise.social.entity.Post;
import com.enterprise.social.entity.SocialProfile;
import com.enterprise.social.repository.ConnectionRepository;
import com.enterprise.social.repository.PostRepository;
import com.enterprise.social.repository.SocialProfileRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SocialService {

    private static final Logger log = LoggerFactory.getLogger(SocialService.class);

    private final SocialProfileRepository profileRepository;
    private final PostRepository postRepository;
    private final ConnectionRepository connectionRepository;
    private final UserGrpcServiceClient userGrpcServiceClient;

    public SocialService(SocialProfileRepository profileRepository,
                         PostRepository postRepository,
                         ConnectionRepository connectionRepository,
                         UserGrpcServiceClient userGrpcServiceClient) {
        this.profileRepository = profileRepository;
        this.postRepository = postRepository;
        this.connectionRepository = connectionRepository;
        this.userGrpcServiceClient = userGrpcServiceClient;
    }

    @Transactional
    @CircuitBreaker(name = "socialService")
    public SocialProfileDTO createOrUpdateProfile(CreateProfileRequest request) {
        log.info("Creating/updating profile for userId={}", request.getUserId());
        validateUserExists(request.getUserId());
        SocialProfile profile = profileRepository.findByUserId(request.getUserId())
                .orElse(new SocialProfile());

        profile.setUserId(request.getUserId());
        profile.setDisplayName(request.getDisplayName());
        profile.setBio(request.getBio());
        profile.setLocation(request.getLocation());
        profile.setWebsite(request.getWebsite());

        SocialProfile saved = profileRepository.save(profile);
        log.info("Profile saved: id={}, userId={}", saved.getId(), saved.getUserId());
        return toProfileDTO(saved);
    }

    @Transactional(readOnly = true)
    @CircuitBreaker(name = "socialService")
    public SocialProfileDTO getProfileByUserId(Long userId) {
        log.debug("Fetching profile for userId={}", userId);
        SocialProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    log.warn("Profile not found for userId={}", userId);
                    return new RuntimeException("Profile not found for userId: " + userId);
                });
        return toProfileDTO(profile);
    }

    @Transactional
    @CircuitBreaker(name = "socialService")
    public PostDTO createPost(CreatePostRequest request) {
        log.info("Creating post for userId={}", request.getUserId());
        validateUserExists(request.getUserId());
        Post post = new Post();
        post.setUserId(request.getUserId());
        post.setContent(request.getContent());

        Post saved = postRepository.save(post);
        log.info("Post created: id={}, userId={}", saved.getId(), saved.getUserId());
        return toPostDTO(saved);
    }

    @Transactional(readOnly = true)
    @CircuitBreaker(name = "socialService")
    public List<PostDTO> getPostsByUserId(Long userId) {
        log.debug("Fetching posts for userId={}", userId);
        return postRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toPostDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    @CircuitBreaker(name = "socialService")
    public ConnectionDTO sendConnectionRequest(CreateConnectionRequest request) {
        log.info("Sending connection request: userId={} -> connectedUserId={}", request.getUserId(), request.getConnectedUserId());
        validateUserExists(request.getUserId());
        validateUserExists(request.getConnectedUserId());
        Connection connection = new Connection();
        connection.setUserId(request.getUserId());
        connection.setConnectedUserId(request.getConnectedUserId());
        connection.setStatus("PENDING");

        Connection saved = connectionRepository.save(connection);
        return toConnectionDTO(saved);
    }

    @Transactional
    @CircuitBreaker(name = "socialService")
    public ConnectionDTO acceptConnection(Long connectionId) {
        log.info("Accepting connection: connectionId={}", connectionId);
        Connection connection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> {
                    log.warn("Connection not found: connectionId={}", connectionId);
                    return new RuntimeException("Connection not found: " + connectionId);
                });

        connection.setStatus("ACCEPTED");
        Connection saved = connectionRepository.save(connection);
        log.info("Connection accepted: id={}, userId={} <-> connectedUserId={}", saved.getId(), saved.getUserId(), saved.getConnectedUserId());

        // Increment followersCount on the connected user's profile
        profileRepository.findByUserId(connection.getConnectedUserId()).ifPresent(profile -> {
            profile.setFollowersCount(profile.getFollowersCount() + 1);
            profileRepository.save(profile);
        });

        // Increment followingCount on the requesting user's profile
        profileRepository.findByUserId(connection.getUserId()).ifPresent(profile -> {
            profile.setFollowingCount(profile.getFollowingCount() + 1);
            profileRepository.save(profile);
        });

        return toConnectionDTO(saved);
    }

    @Transactional(readOnly = true)
    @CircuitBreaker(name = "socialService")
    public List<ConnectionDTO> getConnections(Long userId, String status) {
        log.debug("Fetching connections for userId={}, status={}", userId, status);
        List<Connection> connections;
        if (status != null && !status.isEmpty()) {
            connections = connectionRepository.findByUserIdAndStatus(userId, status);
        } else {
            connections = connectionRepository.findByUserIdAndStatus(userId, "ACCEPTED");
        }
        return connections.stream()
                .map(this::toConnectionDTO)
                .collect(Collectors.toList());
    }

    private SocialProfileDTO toProfileDTO(SocialProfile profile) {
        return new SocialProfileDTO(
                profile.getId(),
                profile.getUserId(),
                profile.getDisplayName(),
                profile.getBio(),
                profile.getLocation(),
                profile.getWebsite(),
                profile.getFollowersCount(),
                profile.getFollowingCount(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }

    private PostDTO toPostDTO(Post post) {
        return new PostDTO(
                post.getId(),
                post.getUserId(),
                post.getContent(),
                post.getLikesCount(),
                post.getCommentsCount(),
                post.getCreatedAt()
        );
    }

    private ConnectionDTO toConnectionDTO(Connection connection) {
        return new ConnectionDTO(
                connection.getId(),
                connection.getUserId(),
                connection.getConnectedUserId(),
                connection.getStatus(),
                connection.getCreatedAt()
        );
    }

    private void validateUserExists(Long userId) {
        try {
            ResponseEntity<GrpcUserDTO> userResponse = userGrpcServiceClient.getUserById(userId);
            if (!userResponse.getStatusCode().is2xxSuccessful() || userResponse.getBody() == null) {
                throw new RuntimeException("User not found with id: " + userId);
            }
            log.debug("User validated for social operation: userId={}, name={}",
                    userId, userResponse.getBody().getName());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to validate user userId={}: {}", userId, e.getMessage());
            throw new RuntimeException("User validation failed: " + e.getMessage());
        }
    }
}
