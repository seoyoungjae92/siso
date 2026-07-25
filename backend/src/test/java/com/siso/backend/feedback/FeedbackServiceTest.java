package com.siso.backend.feedback;

import com.siso.backend.ratelimit.RateLimiter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

    private static final UUID ANON_A = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private FeedbackRepository feedbackRepository;

    @Mock
    private RateLimiter rateLimiter;

    private FeedbackService newService() {
        return new FeedbackService(feedbackRepository, rateLimiter);
    }

    @Test
    void create_savesFeedbackWithNewStatus() {
        FeedbackCreateRequest request = new FeedbackCreateRequest("suggestion", "이런 기능이 있으면 좋겠어요", null);

        newService().create(ANON_A, request);

        ArgumentCaptor<Feedback> captor = ArgumentCaptor.forClass(Feedback.class);
        verify(feedbackRepository).save(captor.capture());
        assertThat(captor.getValue().getCategory()).isEqualTo("suggestion");
        assertThat(captor.getValue().getBody()).isEqualTo("이런 기능이 있으면 좋겠어요");
        assertThat(captor.getValue().getStatus()).isEqualTo("new");
        assertThat(captor.getValue().getContact()).isNull();
    }

    @Test
    void create_trimsBlankContactToNull() {
        FeedbackCreateRequest request = new FeedbackCreateRequest("bug", "버그 있어요", "   ");

        newService().create(ANON_A, request);

        ArgumentCaptor<Feedback> captor = ArgumentCaptor.forClass(Feedback.class);
        verify(feedbackRepository).save(captor.capture());
        assertThat(captor.getValue().getContact()).isNull();
    }

    @Test
    void create_keepsProvidedContact() {
        FeedbackCreateRequest request = new FeedbackCreateRequest("etc", "문의합니다", " test@example.com ");

        newService().create(ANON_A, request);

        ArgumentCaptor<Feedback> captor = ArgumentCaptor.forClass(Feedback.class);
        verify(feedbackRepository).save(captor.capture());
        assertThat(captor.getValue().getContact()).isEqualTo("test@example.com");
    }

    @Test
    void create_rejectsInvalidCategory() {
        FeedbackCreateRequest request = new FeedbackCreateRequest("invalid", "본문", null);

        assertThatThrownBy(() -> newService().create(ANON_A, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.BAD_REQUEST);
        verify(feedbackRepository, never()).save(any(Feedback.class));
    }

    @Test
    void create_rejectsBlankBody() {
        FeedbackCreateRequest request = new FeedbackCreateRequest("report", "   ", null);

        assertThatThrownBy(() -> newService().create(ANON_A, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.BAD_REQUEST);
        verify(feedbackRepository, never()).save(any(Feedback.class));
    }

    @Test
    void create_enforcesRateLimit() {
        FeedbackCreateRequest request = new FeedbackCreateRequest("report", "본문", null);
        doThrow(new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS))
                .when(rateLimiter)
                .checkOrThrow("feedback", ANON_A);

        assertThatThrownBy(() -> newService().create(ANON_A, request))
                .isInstanceOf(ResponseStatusException.class);
        verify(feedbackRepository, never()).save(any(Feedback.class));
    }
}
