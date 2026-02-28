package com.secure.user.controller;

import com.secure.common.dto.ApiResponse;
import com.secure.user.dto.UpdateProfileRequest;
import com.secure.user.dto.UserProfileDTO;
import com.secure.user.service.SecurityService;
import com.secure.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private SecurityService securityService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserProfileDTO>>> getAllProfiles() {
        log.debug("GET /users");
        List<UserProfileDTO> profiles = userService.getAllProfiles();
        ApiResponse<List<UserProfileDTO>> response = ApiResponse.<List<UserProfileDTO>>builder()
                .success(true)
                .message("Profiles retrieved successfully")
                .data(profiles)
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @securityService.isOwner(#id)")
    public ResponseEntity<ApiResponse<UserProfileDTO>> getProfileById(@PathVariable Long id) {
        log.debug("GET /users/{}", id);
        UserProfileDTO profile = userService.getProfileById(id);
        ApiResponse<UserProfileDTO> response = ApiResponse.<UserProfileDTO>builder()
                .success(true)
                .message("Profile retrieved successfully")
                .data(profile)
                .build();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@securityService.isOwner(#id)")
    public ResponseEntity<ApiResponse<UserProfileDTO>> updateProfile(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProfileRequest request) {
        log.info("PUT /users/{}", id);
        UserProfileDTO updatedProfile = userService.updateProfile(id, request);
        ApiResponse<UserProfileDTO> response = ApiResponse.<UserProfileDTO>builder()
                .success(true)
                .message("Profile updated successfully")
                .data(updatedProfile)
                .build();
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteProfile(@PathVariable Long id) {
        log.info("DELETE /users/{}", id);
        userService.deleteProfile(id);
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(true)
                .message("Profile deleted successfully")
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileDTO>> getCurrentUserProfile() {
        log.debug("GET /users/me");
        Long currentUserId = securityService.getCurrentUserId();
        if (currentUserId == null) {
            ApiResponse<UserProfileDTO> response = ApiResponse.<UserProfileDTO>builder()
                    .success(false)
                    .message("User not authenticated")
                    .build();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        UserProfileDTO profile = userService.getOrCreateProfileByUserId(currentUserId);
        ApiResponse<UserProfileDTO> response = ApiResponse.<UserProfileDTO>builder()
                .success(true)
                .message("Profile retrieved successfully")
                .data(profile)
                .build();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileDTO>> updateCurrentUserProfile(
            @Valid @RequestBody UpdateProfileRequest request) {
        log.info("PUT /users/me");
        Long currentUserId = securityService.getCurrentUserId();
        if (currentUserId == null) {
            ApiResponse<UserProfileDTO> response = ApiResponse.<UserProfileDTO>builder()
                    .success(false)
                    .message("User not authenticated")
                    .build();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        UserProfileDTO updatedProfile = userService.upsertProfileByUserId(currentUserId, request);
        ApiResponse<UserProfileDTO> response = ApiResponse.<UserProfileDTO>builder()
                .success(true)
                .message("Profile updated successfully")
                .data(updatedProfile)
                .build();
        return ResponseEntity.ok(response);
    }
}
