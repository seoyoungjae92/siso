package com.siso.backend.comment;

import com.siso.backend.anon.AnonIdHeader;
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

import java.util.List;

@RestController
public class CommentController {

    private final CommentService commentService;
    private final ReportService reportService;

    public CommentController(CommentService commentService, ReportService reportService) {
        this.commentService = commentService;
        this.reportService = reportService;
    }

    @GetMapping("/api/pairs/{pairId}/comments")
    public List<CommentDto> getComments(
            @PathVariable Long pairId,
            @RequestParam(defaultValue = "top") String sort,
            @RequestHeader(value = "X-Anon-Id", required = false) String anonId) {
        return commentService.getComments(pairId, sort, AnonIdHeader.parse(anonId, false));
    }

    @PostMapping("/api/pairs/{pairId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentDto createComment(
            @PathVariable Long pairId,
            @RequestHeader(value = "X-Anon-Id", required = false) String anonId,
            @RequestBody CommentCreateRequest request,
            HttpServletRequest servletRequest) {
        return commentService.create(
                pairId,
                AnonIdHeader.parse(anonId, true),
                servletRequest.getRemoteAddr(),
                request.parentId(),
                request.body(),
                request.stance());
    }

    @PostMapping("/api/comments/{commentId}/reactions")
    public void react(
            @PathVariable Long commentId,
            @RequestHeader(value = "X-Anon-Id", required = false) String anonId,
            @RequestBody ReactionCreateRequest request) {
        commentService.react(commentId, AnonIdHeader.parse(anonId, true), request.type());
    }

    @PostMapping("/api/comments/{commentId}/reports")
    @ResponseStatus(HttpStatus.CREATED)
    public void report(
            @PathVariable Long commentId,
            @RequestHeader(value = "X-Anon-Id", required = false) String anonId,
            @RequestBody ReportCreateRequest request) {
        reportService.create(commentId, AnonIdHeader.parse(anonId, true), request.reason(), request.detail());
    }
}
