package com.enterprise.social.controller;

import com.enterprise.social.dto.CreatePostRequest;
import com.enterprise.social.dto.PostDTO;
import com.enterprise.social.service.SocialService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private static final Logger log = LoggerFactory.getLogger(PostController.class);

    private final SocialService socialService;

    public PostController(SocialService socialService) {
        this.socialService = socialService;
    }

    @PostMapping
    public ResponseEntity<PostDTO> createPost(@Valid @RequestBody CreatePostRequest request) {
        log.info("POST /api/posts - userId={}", request.getUserId());
        PostDTO post = socialService.createPost(request);
        return ResponseEntity.ok(post);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PostDTO>> getPostsByUserId(@PathVariable Long userId) {
        log.debug("GET /api/posts/user/{}", userId);
        List<PostDTO> posts = socialService.getPostsByUserId(userId);
        return ResponseEntity.ok(posts);
    }
}
