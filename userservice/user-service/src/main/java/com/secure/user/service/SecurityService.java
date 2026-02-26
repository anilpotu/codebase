package com.secure.user.service;

import com.secure.user.entity.UserProfile;
import com.secure.user.repository.UserProfileRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class SecurityService {

    @Autowired
    private UserProfileRepository userProfileRepository;

    public boolean isOwner(Long profileId) {
        log.debug("Checking ownership for profileId={}", profileId);
        Long currentUserId = getCurrentUserId();
        log.debug("Current userId={}", currentUserId);
        if (currentUserId == null) {
            return false;
        }

        Optional<UserProfile> profile = userProfileRepository.findById(profileId);
        return profile.isPresent() && profile.get().getUserId().equals(currentUserId);
    }

    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof Jwt) {
            Jwt jwt = (Jwt) principal;
            Object userIdClaim = jwt.getClaim("userId");

            if (userIdClaim instanceof Integer) {
                return ((Integer) userIdClaim).longValue();
            } else if (userIdClaim instanceof Long) {
                return (Long) userIdClaim;
            } else if (userIdClaim instanceof String) {
                return Long.parseLong((String) userIdClaim);
            }
        }

        return null;
    }
}
