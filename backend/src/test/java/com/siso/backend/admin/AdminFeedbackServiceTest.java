package com.siso.backend.admin;

import com.siso.backend.feedback.Feedback;
import com.siso.backend.feedback.FeedbackRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminFeedbackServiceTest {

    @Mock
    private FeedbackRepository feedbackRepository;

    private AdminFeedbackService newService() {
        return new AdminFeedbackService(feedbackRepository);
    }

    private Feedback feedback(String category, String body) {
        Feedback f = new Feedback(category, body, null, UUID.randomUUID(), OffsetDateTime.now());
        ReflectionTestUtils.setField(f, "id", 1L);
        return f;
    }

    @Test
    void list_passesFiltersThrough() {
        when(feedbackRepository.findByFilters(eq("bug"), eq("new"), any(Pageable.class)))
                .thenReturn(List.of(feedback("bug", "버그 리포트")));

        List<AdminFeedbackDto> result = newService().list("bug", "new");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).category()).isEqualTo("bug");
        assertThat(result.get(0).body()).isEqualTo("버그 리포트");
    }

    @Test
    void list_allowsNullFilters() {
        when(feedbackRepository.findByFilters(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(List.of());

        List<AdminFeedbackDto> result = newService().list(null, null);

        assertThat(result).isEmpty();
    }

    @Test
    void resolve_marksFeedbackResolved() {
        Feedback f = feedback("suggestion", "건의합니다");
        when(feedbackRepository.findById(1L)).thenReturn(Optional.of(f));

        newService().resolve(1L);

        assertThat(f.getStatus()).isEqualTo("resolved");
    }

    @Test
    void resolve_missingFeedback_throwsNotFound() {
        when(feedbackRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> newService().resolve(99L)).isInstanceOf(ResponseStatusException.class);
    }
}
