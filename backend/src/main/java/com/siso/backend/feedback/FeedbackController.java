package com.siso.backend.feedback;

import com.siso.backend.anon.AnonIdHeader;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping("/api/feedback")
    @ResponseStatus(HttpStatus.CREATED)
    public void create(
            @RequestHeader(value = "X-Anon-Id", required = false) String anonId,
            @Valid @RequestBody FeedbackCreateRequest request) {
        feedbackService.create(AnonIdHeader.parse(anonId, true), request);
    }
}
