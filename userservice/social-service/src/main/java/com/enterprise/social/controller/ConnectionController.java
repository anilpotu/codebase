package com.enterprise.social.controller;

import com.enterprise.social.dto.ConnectionDTO;
import com.enterprise.social.dto.CreateConnectionRequest;
import com.enterprise.social.service.SocialService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/connections")
public class ConnectionController {

    private static final Logger log = LoggerFactory.getLogger(ConnectionController.class);

    private final SocialService socialService;

    public ConnectionController(SocialService socialService) {
        this.socialService = socialService;
    }

    @PostMapping
    public ResponseEntity<ConnectionDTO> sendConnectionRequest(@Valid @RequestBody CreateConnectionRequest request) {
        log.info("POST /api/connections - userId={} -> connectedUserId={}", request.getUserId(), request.getConnectedUserId());
        ConnectionDTO connection = socialService.sendConnectionRequest(request);
        return ResponseEntity.ok(connection);
    }

    @PutMapping("/{id}/accept")
    public ResponseEntity<ConnectionDTO> acceptConnection(@PathVariable Long id) {
        log.info("PUT /api/connections/{}/accept", id);
        ConnectionDTO connection = socialService.acceptConnection(id);
        return ResponseEntity.ok(connection);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ConnectionDTO>> getConnections(
            @PathVariable Long userId,
            @RequestParam(required = false) String status) {
        log.debug("GET /api/connections/user/{} status={}", userId, status);
        List<ConnectionDTO> connections = socialService.getConnections(userId, status);
        return ResponseEntity.ok(connections);
    }
}
