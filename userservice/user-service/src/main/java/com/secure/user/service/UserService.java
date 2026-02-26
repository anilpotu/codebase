package com.secure.user.service;

import com.secure.user.dto.UpdateProfileRequest;
import com.secure.user.dto.UserProfileDTO;
import com.secure.user.entity.UserProfile;
import com.secure.user.repository.UserProfileRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class UserService {

    @Autowired
    private UserProfileRepository userProfileRepository;

    public List<UserProfileDTO> getAllProfiles() {
        log.debug("Fetching all user profiles");
        return userProfileRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public UserProfileDTO getProfileById(Long id) {
        log.debug("Fetching user profile: {}", id);
        UserProfile profile = userProfileRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("User profile not found with id: {}", id);
                    return new EntityNotFoundException("User profile not found with id: " + id);
                });
        return convertToDTO(profile);
    }

    public UserProfileDTO getProfileByUserId(Long userId) {
        log.debug("Fetching user profile by userId: {}", userId);
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    log.warn("User profile not found for userId: {}", userId);
                    return new EntityNotFoundException("User profile not found for userId: " + userId);
                });
        return convertToDTO(profile);
    }

    public UserProfileDTO updateProfile(Long id, UpdateProfileRequest request) {
        log.info("Updating user profile: {}", id);
        UserProfile profile = userProfileRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("User profile not found with id: {}", id);
                    return new EntityNotFoundException("User profile not found with id: " + id);
                });

        profile.setFirstName(request.getFirstName());
        profile.setLastName(request.getLastName());
        profile.setPhoneNumber(request.getPhoneNumber());
        profile.setAddress(request.getAddress());

        UserProfile updatedProfile = userProfileRepository.save(profile);
        return convertToDTO(updatedProfile);
    }

    public void deleteProfile(Long id) {
        log.info("Deleting user profile: {}", id);
        if (!userProfileRepository.existsById(id)) {
            log.warn("User profile not found with id: {}", id);
            throw new EntityNotFoundException("User profile not found with id: " + id);
        }
        userProfileRepository.deleteById(id);
    }

    private UserProfileDTO convertToDTO(UserProfile profile) {
        return UserProfileDTO.builder()
                .id(profile.getId())
                .userId(profile.getUserId())
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .phoneNumber(profile.getPhoneNumber())
                .address(profile.getAddress())
                .build();
    }
}
