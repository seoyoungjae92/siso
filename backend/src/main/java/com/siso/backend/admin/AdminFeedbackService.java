package com.siso.backend.admin;

import com.siso.backend.feedback.Feedback;
import com.siso.backend.feedback.FeedbackRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AdminFeedbackService {

    private static final int LIST_LIMIT = 100;

    private final FeedbackRepository feedbackRepository;

    public AdminFeedbackService(FeedbackRepository feedbackRepository) {
        this.feedbackRepository = feedbackRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminFeedbackDto> list(String category, String status) {
        return feedbackRepository.findByFilters(category, status, PageRequest.of(0, LIST_LIMIT)).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public void resolve(Long id) {
        Feedback feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "feedback not found"));
        feedback.resolve();
    }

    private AdminFeedbackDto toDto(Feedback feedback) {
        return new AdminFeedbackDto(
                feedback.getId(),
                feedback.getCategory(),
                feedback.getBody(),
                feedback.getContact(),
                feedback.getStatus(),
                feedback.getCreatedAt());
    }
}
