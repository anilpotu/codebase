package com.enterprise.social.repository;

import com.enterprise.social.entity.SocialProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SocialProfileRepository extends JpaRepository<SocialProfile, Long> {

    Optional<SocialProfile> findByUserId(Long userId);
}
