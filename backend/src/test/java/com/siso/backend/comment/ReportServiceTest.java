package com.siso.backend.comment;

import com.siso.backend.alert.AdminAlert;
import com.siso.backend.alert.AdminAlertRepository;
import com.siso.backend.pair.TopicPair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    private static final UUID ANON_A = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private AdminAlertRepository adminAlertRepository;

    private ReportService newService() {
        return new ReportService(reportRepository, commentRepository, adminAlertRepository);
    }

    private Comment commentWithId(long id) {
        TopicPair pair = Mockito.mock(TopicPair.class);
        Comment comment = new Comment(pair, null, ANON_A, "닉네임", "본문", "hash", null, OffsetDateTime.now());
        ReflectionTestUtils.setField(comment, "id", id);
        return comment;
    }

    @Test
    void create_savesReport() {
        ReportService service = newService();
        Comment comment = commentWithId(1L);
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(reportRepository.existsByComment_IdAndAnonId(1L, ANON_A)).thenReturn(false);

        service.create(1L, ANON_A, "abuse", "인신공격성 표현입니다");

        verify(reportRepository).save(any(Report.class));
    }

    @Test
    void create_duplicateReportBySameAnon_isRejected() {
        ReportService service = newService();
        Comment comment = commentWithId(1L);
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(reportRepository.existsByComment_IdAndAnonId(1L, ANON_A)).thenReturn(true);

        assertThatThrownBy(() -> service.create(1L, ANON_A, "abuse", null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("이미 신고");
    }

    @Test
    void create_invalidReason_isRejected() {
        ReportService service = newService();

        assertThatThrownBy(() -> service.create(1L, ANON_A, "not-a-reason", null))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void create_reachesThreshold_autoBlindsAndLogsAlert() {
        ReportService service = newService();
        Comment comment = commentWithId(1L);
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(reportRepository.existsByComment_IdAndAnonId(1L, ANON_A)).thenReturn(false);
        when(reportRepository.countByComment_Id(1L)).thenReturn(20L);
        when(reportRepository.findByStatusAndComment_Id("pending", 1L)).thenReturn(List.of());

        service.create(1L, ANON_A, "abuse", null);

        assertThat(comment.getStatus()).isEqualTo("blinded");
        verify(adminAlertRepository).save(any(AdminAlert.class));
    }

    @Test
    void create_belowThreshold_doesNotAutoBlind() {
        ReportService service = newService();
        Comment comment = commentWithId(1L);
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(reportRepository.existsByComment_IdAndAnonId(1L, ANON_A)).thenReturn(false);
        when(reportRepository.countByComment_Id(1L)).thenReturn(19L);

        service.create(1L, ANON_A, "abuse", null);

        assertThat(comment.getStatus()).isEqualTo("visible");
        verify(adminAlertRepository, never()).save(any(AdminAlert.class));
    }

    @Test
    void create_alreadyBlinded_doesNotLogDuplicateAlert() {
        ReportService service = newService();
        Comment comment = commentWithId(1L);
        comment.blind();
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(reportRepository.existsByComment_IdAndAnonId(1L, ANON_A)).thenReturn(false);
        when(reportRepository.countByComment_Id(1L)).thenReturn(21L);

        service.create(1L, ANON_A, "abuse", null);

        verify(adminAlertRepository, never()).save(any(AdminAlert.class));
    }
}
