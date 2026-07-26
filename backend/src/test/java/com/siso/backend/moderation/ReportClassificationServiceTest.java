package com.siso.backend.moderation;

import com.siso.backend.comment.Comment;
import com.siso.backend.comment.CommentRepository;
import com.siso.backend.comment.Report;
import com.siso.backend.comment.ReportRepository;
import com.siso.backend.pair.TopicPair;
import com.siso.backend.settings.ModerationSettings;
import com.siso.backend.settings.ModerationSettingsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportClassificationServiceTest {

    private static final UUID ANON_A = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private ModerationSettingsRepository moderationSettingsRepository;

    @Mock
    private OpenRouterReportClassifier classifier;

    private ReportClassificationService newService() {
        return new ReportClassificationService(reportRepository, commentRepository, moderationSettingsRepository, classifier);
    }

    private Comment commentWithId(long id) {
        TopicPair pair = Mockito.mock(TopicPair.class);
        Comment comment = new Comment(pair, null, ANON_A, "닉네임", "신고당한 댓글", "hash", null, OffsetDateTime.now());
        ReflectionTestUtils.setField(comment, "id", id);
        return comment;
    }

    private void stubModel(String model) {
        ModerationSettings settings = Mockito.mock(ModerationSettings.class);
        when(settings.getClassificationModel()).thenReturn(model);
        when(moderationSettingsRepository.findById((short) 1)).thenReturn(Optional.of(settings));
    }

    @Test
    void classifyPending_classifierDisabled_doesNothing() {
        when(classifier.isEnabled()).thenReturn(false);

        int result = newService().classifyPending();

        assertThat(result).isZero();
        verify(reportRepository, never()).findDistinctCommentIdsWithUnclassifiedPendingReports(any());
    }

    @Test
    void classifyPending_obviousViolation_appliesClassificationToComment() {
        when(classifier.isEnabled()).thenReturn(true);
        stubModel("openrouter/free");
        Comment comment = commentWithId(1L);
        when(reportRepository.findDistinctCommentIdsWithUnclassifiedPendingReports(any(Pageable.class)))
                .thenReturn(List.of(1L));
        when(commentRepository.findAllById(List.of(1L))).thenReturn(List.of(comment));
        when(reportRepository.findByStatusAndComment_Id("pending", 1L))
                .thenReturn(List.of(new Report(comment, ANON_A, "abuse", "욕설입니다", OffsetDateTime.now())));
        when(classifier.classify("openrouter/free", "신고당한 댓글", List.of("abuse"), List.of("욕설입니다")))
                .thenReturn(new ReportClassification("obvious_violation", "명백한 욕설 표현"));

        int result = newService().classifyPending();

        assertThat(result).isEqualTo(1);
        assertThat(comment.getLlmVerdict()).isEqualTo("obvious_violation");
        assertThat(comment.getLlmReason()).isEqualTo("명백한 욕설 표현");
        assertThat(comment.getLlmClassifiedAt()).isNotNull();
        // 힌트만 제공 — 실제 상태 변경(블라인드)은 하지 않는다
        assertThat(comment.getStatus()).isEqualTo("visible");
    }

    @Test
    void classifyPending_classifierFails_skipsCommentAndLeavesUnclassified() {
        when(classifier.isEnabled()).thenReturn(true);
        stubModel("openrouter/free");
        Comment comment = commentWithId(1L);
        when(reportRepository.findDistinctCommentIdsWithUnclassifiedPendingReports(any(Pageable.class)))
                .thenReturn(List.of(1L));
        when(commentRepository.findAllById(List.of(1L))).thenReturn(List.of(comment));
        when(reportRepository.findByStatusAndComment_Id("pending", 1L))
                .thenReturn(List.of(new Report(comment, ANON_A, "abuse", null, OffsetDateTime.now())));
        when(classifier.classify(any(), any(), any(), any())).thenThrow(new ReportClassificationFailed("API 에러"));

        int result = newService().classifyPending();

        assertThat(result).isZero();
        assertThat(comment.getLlmVerdict()).isNull();
    }

    @Test
    void classifyPending_noPendingReportsLeftForCandidate_skipsWithoutCallingClassifier() {
        when(classifier.isEnabled()).thenReturn(true);
        stubModel("openrouter/free");
        Comment comment = commentWithId(1L);
        when(reportRepository.findDistinctCommentIdsWithUnclassifiedPendingReports(any(Pageable.class)))
                .thenReturn(List.of(1L));
        when(commentRepository.findAllById(List.of(1L))).thenReturn(List.of(comment));
        when(reportRepository.findByStatusAndComment_Id("pending", 1L)).thenReturn(List.of());

        int result = newService().classifyPending();

        assertThat(result).isZero();
        verify(classifier, never()).classify(any(), any(), any(), any());
    }
}
