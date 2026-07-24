package com.siso.backend.abuse;

import com.siso.backend.comment.CommentRepository;
import com.siso.backend.moderation.DuplicateCommentFilter;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class DuplicateCommentGuard {

    private final CommentRepository commentRepository;
    private final DuplicateCommentFilter duplicateCommentFilter;

    public DuplicateCommentGuard(CommentRepository commentRepository, DuplicateCommentFilter duplicateCommentFilter) {
        this.commentRepository = commentRepository;
        this.duplicateCommentFilter = duplicateCommentFilter;
    }

    public boolean isDuplicate(
            UUID anonId, String body, OffsetDateTime now, int lookbackMinutes, int lookbackCount, double threshold) {
        List<String> recent = commentRepository.findRecentBodiesByAnonId(
                anonId, now.minusMinutes(lookbackMinutes), PageRequest.of(0, lookbackCount));
        return duplicateCommentFilter.isDuplicate(body, recent, threshold);
    }
}
