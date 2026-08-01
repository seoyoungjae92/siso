package com.siso.backend.comment;

import com.siso.backend.anon.AnonIdHeader;
import com.siso.backend.anon.AnonIdSigner;
import com.siso.backend.anon.ClientIp;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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
    private final AnonIdSigner anonIdSigner;

    public CommentController(
            CommentService commentService, ReportService reportService, AnonIdSigner anonIdSigner) {
        this.commentService = commentService;
        this.reportService = reportService;
        this.anonIdSigner = anonIdSigner;
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
            @RequestHeader(value = "X-Anon-Sig", required = false) String anonSig,
            @Valid @RequestBody CommentCreateRequest request,
            HttpServletRequest servletRequest) {
        return commentService.create(
                pairId,
                AnonIdHeader.parseAndVerify(anonId, anonSig, anonIdSigner),
                ClientIp.resolve(servletRequest),
                request.parentId(),
                request.body(),
                request.stance());
    }

    @PostMapping("/api/comments/{commentId}/reactions")
    public void react(
            @PathVariable Long commentId,
            @RequestHeader(value = "X-Anon-Id", required = false) String anonId,
            @RequestHeader(value = "X-Anon-Sig", required = false) String anonSig,
            @RequestBody ReactionCreateRequest request) {
        commentService.react(commentId, AnonIdHeader.parseAndVerify(anonId, anonSig, anonIdSigner), request.type());
    }

    @PostMapping("/api/comments/{commentId}/reports")
    @ResponseStatus(HttpStatus.CREATED)
    public void report(
            @PathVariable Long commentId,
            @RequestHeader(value = "X-Anon-Id", required = false) String anonId,
            @RequestHeader(value = "X-Anon-Sig", required = false) String anonSig,
            @Valid @RequestBody ReportCreateRequest request) {
        reportService.create(
                commentId, AnonIdHeader.parseAndVerify(anonId, anonSig, anonIdSigner), request.reason(), request.detail());
    }
}
