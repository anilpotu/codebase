package com.enterprise.social.repository;

import com.enterprise.social.entity.Connection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConnectionRepository extends JpaRepository<Connection, Long> {

    List<Connection> findByUserIdAndStatus(Long userId, String status);

    Optional<Connection> findByUserIdAndConnectedUserId(Long userId, Long connectedUserId);
}
