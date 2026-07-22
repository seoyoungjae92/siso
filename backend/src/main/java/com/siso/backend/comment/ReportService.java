package com.siso.backend.comment;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

@Service
public class ReportService {

    private static final Set<String> REASONS = Set.of("abuse", "hate", "spam", "etc");

    private final ReportRepository reportRepository;
    private final CommentRepository commentRepository;

    public ReportService(ReportRepository reportRepository, CommentRepository commentRepository) {
        this.reportRepository = reportRepository;
        this.commentRepository = commentRepository;
    }

    @Transactional
    public void create(Long commentId, UUID anonId, String reason, String detail) {
        if (!REASONS.contains(reason)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "reason must be abuse, hate, spam, or etc");
        }

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "comment not found"));

        if (reportRepository.existsByComment_IdAndAnonId(commentId, anonId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 신고한 댓글입니다");
        }

        reportRepository.save(new Report(comment, anonId, reason, detail, OffsetDateTime.now()));
    }
}
