package com.siso.backend.feedback;

import com.siso.backend.anon.AnonIdHeader;
import com.siso.backend.anon.AnonIdSigner;
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
    private final AnonIdSigner anonIdSigner;

    public FeedbackController(FeedbackService feedbackService, AnonIdSigner anonIdSigner) {
        this.feedbackService = feedbackService;
        this.anonIdSigner = anonIdSigner;
    }

    @PostMapping("/api/feedback")
    @ResponseStatus(HttpStatus.CREATED)
    public void create(
            @RequestHeader(value = "X-Anon-Id", required = false) String anonId,
            @RequestHeader(value = "X-Anon-Sig", required = false) String anonSig,
            @Valid @RequestBody FeedbackCreateRequest request) {
        feedbackService.create(AnonIdHeader.parseAndVerify(anonId, anonSig, anonIdSigner), request);
    }
}
