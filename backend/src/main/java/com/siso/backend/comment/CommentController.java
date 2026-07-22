package com.siso.backend.comment;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping("/api/pairs/{pairId}/comments")
    public List<CommentDto> getComments(
            @PathVariable Long pairId,
            @RequestParam(defaultValue = "top") String sort) {
        return commentService.getComments(pairId, sort);
    }

    @PostMapping("/api/pairs/{pairId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentDto createComment(
            @PathVariable Long pairId,
            @RequestHeader(value = "X-Anon-Id", required = false) String anonId,
            @RequestBody CommentCreateRequest request,
            HttpServletRequest servletRequest) {
        if (anonId == null || anonId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "X-Anon-Id header is required");
        }

        UUID parsedAnonId;
        try {
            parsedAnonId = UUID.fromString(anonId);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "X-Anon-Id must be a UUID");
        }

        return commentService.create(
                pairId,
                parsedAnonId,
                servletRequest.getRemoteAddr(),
                request.parentId(),
                request.body(),
                request.stance());
    }
}
