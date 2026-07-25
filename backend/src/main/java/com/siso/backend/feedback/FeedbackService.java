package com.siso.backend.feedback;

import com.siso.backend.ratelimit.RateLimiter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

@Service
public class FeedbackService {

    private static final Set<String> CATEGORIES = Set.of("suggestion", "report", "bug", "etc");

    private final FeedbackRepository feedbackRepository;
    private final RateLimiter rateLimiter;

    public FeedbackService(FeedbackRepository feedbackRepository, RateLimiter rateLimiter) {
        this.feedbackRepository = feedbackRepository;
        this.rateLimiter = rateLimiter;
    }

    @Transactional
    public void create(UUID anonId, FeedbackCreateRequest request) {
        if (!CATEGORIES.contains(request.category())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "category must be suggestion, report, bug, or etc");
        }
        if (request.body() == null || request.body().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "본문을 입력해주세요");
        }

        rateLimiter.checkOrThrow("feedback", anonId);

        String contact = request.contact() == null || request.contact().isBlank() ? null : request.contact().trim();
        feedbackRepository.save(new Feedback(request.category(), request.body(), contact, anonId, OffsetDateTime.now()));
    }
}
