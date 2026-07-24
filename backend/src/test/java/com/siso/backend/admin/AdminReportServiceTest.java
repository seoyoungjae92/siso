package com.siso.backend.admin;

import com.siso.backend.alert.AdminAlert;
import com.siso.backend.alert.AdminAlertRepository;
import com.siso.backend.comment.Comment;
import com.siso.backend.comment.CommentRepository;
import com.siso.backend.comment.Report;
import com.siso.backend.comment.ReportRepository;
import com.siso.backend.pair.TopicPair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminReportServiceTest {

    private static final UUID ANON_A = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ANON_B = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID ANON_C = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private AdminAlertRepository adminAlertRepository;

    private AdminReportService newService() {
        return new AdminReportService(reportRepository, commentRepository, adminAlertRepository);
    }

    private Comment commentWithId(long id) {
        TopicPair pair = Mockito.mock(TopicPair.class);
        Comment comment = new Comment(pair, null, ANON_A, "닉네임", "신고당한 댓글", "hash", null, OffsetDateTime.now());
        ReflectionTestUtils.setField(comment, "id", id);
        return comment;
    }

    @Test
    void getPendingGroupedByComment_multipleReportsOnSameComment_groupedIntoOne() {
        Comment comment = commentWithId(1L);
        when(comment.getPair().getId()).thenReturn(1L);
        Report r1 = new Report(comment, ANON_A, "abuse", null, OffsetDateTime.now().minusMinutes(3));
        Report r2 = new Report(comment, ANON_B, "abuse", null, OffsetDateTime.now().minusMinutes(2));
        Report r3 = new Report(comment, ANON_C, "spam", null, OffsetDateTime.now().minusMinutes(1));
        when(reportRepository.findByStatusOrderByCreatedAtAsc("pending")).thenReturn(List.of(r1, r2, r3));

        List<PendingReportGroupDto> result = newService().getPendingGroupedByComment();

        assertThat(result).hasSize(1);
        PendingReportGroupDto group = result.get(0);
        assertThat(group.commentId()).isEqualTo(1L);
        assertThat(group.totalReports()).isEqualTo(3);
        assertThat(group.reasonCounts()).containsEntry("abuse", 2L).containsEntry("spam", 1L);
        assertThat(group.oldestReportAt()).isEqualTo(r1.getCreatedAt());
    }

    @Test
    void moderate_blind_blindsCommentAndAcceptsAllPendingReports() {
        Comment comment = commentWithId(1L);
        Report r1 = new Report(comment, ANON_A, "abuse", null, OffsetDateTime.now());
        Report r2 = new Report(comment, ANON_B, "spam", null, OffsetDateTime.now());
        when(reportRepository.findByStatusAndComment_Id("pending", 1L)).thenReturn(List.of(r1, r2));

        newService().moderate(1L, "blind");

        assertThat(comment.getStatus()).isEqualTo("blinded");
        assertThat(r1.getStatus()).isEqualTo("accepted");
        assertThat(r2.getStatus()).isEqualTo("accepted");

        ArgumentCaptor<AdminAlert> captor = ArgumentCaptor.forClass(AdminAlert.class);
        verify(adminAlertRepository).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo("comment_manually_blinded");
        assertThat(captor.getValue().getPayload()).containsEntry("commentId", 1L).containsEntry("reportCount", 2);
    }

    @Test
    void moderate_dismiss_rejectsReportsWithoutTouchingComment() {
        Comment comment = commentWithId(1L);
        Report r1 = new Report(comment, ANON_A, "abuse", null, OffsetDateTime.now());
        when(reportRepository.findByStatusAndComment_Id("pending", 1L)).thenReturn(List.of(r1));

        newService().moderate(1L, "dismiss");

        assertThat(comment.getStatus()).isEqualTo("visible");
        assertThat(r1.getStatus()).isEqualTo("rejected");
        verify(adminAlertRepository, never()).save(any(AdminAlert.class));
    }

    @Test
    void moderate_invalidAction_isRejected() {
        Comment comment = commentWithId(1L);
        Report r1 = new Report(comment, ANON_A, "abuse", null, OffsetDateTime.now());
        when(reportRepository.findByStatusAndComment_Id("pending", 1L)).thenReturn(List.of(r1));

        assertThatThrownBy(() -> newService().moderate(1L, "delete"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void moderate_noPendingReportsForComment_isNotFound() {
        when(reportRepository.findByStatusAndComment_Id("pending", 1L)).thenReturn(List.of());

        assertThatThrownBy(() -> newService().moderate(1L, "blind"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getBlindHistory_joinsCommentDetailsAndOrdersByNewestFirst() {
        Comment comment = commentWithId(1L);
        when(comment.getPair().getId()).thenReturn(5L);
        Map<String, Object> payload = Map.of("commentId", 1L, "pairId", 5L, "reportCount", 20);
        AdminAlert alert = new AdminAlert("comment_auto_blinded", payload, OffsetDateTime.now());
        when(adminAlertRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(alert));
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

        List<BlindHistoryDto> history = newService().getBlindHistory();

        assertThat(history).hasSize(1);
        BlindHistoryDto entry = history.get(0);
        assertThat(entry.type()).isEqualTo("comment_auto_blinded");
        assertThat(entry.commentId()).isEqualTo(1L);
        assertThat(entry.commentBody()).isEqualTo("신고당한 댓글");
        assertThat(entry.pairId()).isEqualTo(5L);
        assertThat(entry.reportCount()).isEqualTo(20);
    }
}
