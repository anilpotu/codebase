package com.enterprise.social.controller;

import com.enterprise.social.dto.CreateProfileRequest;
import com.enterprise.social.dto.SocialProfileDTO;
import com.enterprise.social.service.SocialService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/profiles")
public class ProfileController {

    private static final Logger log = LoggerFactory.getLogger(ProfileController.class);

    private final SocialService socialService;

    public ProfileController(SocialService socialService) {
        this.socialService = socialService;
    }

    @PostMapping
    public ResponseEntity<SocialProfileDTO> createOrUpdateProfile(@Valid @RequestBody CreateProfileRequest request) {
        log.info("POST /api/profiles - userId={}", request.getUserId());
        SocialProfileDTO profile = socialService.createOrUpdateProfile(request);
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<SocialProfileDTO> getProfileByUserId(@PathVariable Long userId) {
        log.debug("GET /api/profiles/user/{}", userId);
        SocialProfileDTO profile = socialService.getProfileByUserId(userId);
        return ResponseEntity.ok(profile);
    }
}
