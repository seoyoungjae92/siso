package com.siso.backend.retention;

import com.siso.backend.anon.AnonUserRepository;
import com.siso.backend.comment.CommentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RetentionPurgeServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private AnonUserRepository anonUserRepository;

    private RetentionPurgeService newService() {
        return new RetentionPurgeService(commentRepository, anonUserRepository);
    }

    @Test
    void purge_returnsCountsFromBothRepositories() {
        when(commentRepository.purgeIpHashOlderThan(any(OffsetDateTime.class))).thenReturn(7);
        when(anonUserRepository.purgeIpHashOlderThan(any(OffsetDateTime.class))).thenReturn(3);

        RetentionPurgeResult result = newService().purge();

        assertThat(result.commentsPurged()).isEqualTo(7);
        assertThat(result.anonUsersPurged()).isEqualTo(3);
    }

    @Test
    void purge_usesNinetyDayCutoffForBothRepositories() {
        when(commentRepository.purgeIpHashOlderThan(any(OffsetDateTime.class))).thenReturn(0);
        when(anonUserRepository.purgeIpHashOlderThan(any(OffsetDateTime.class))).thenReturn(0);

        OffsetDateTime before = OffsetDateTime.now().minusDays(90);
        newService().purge();
        OffsetDateTime after = OffsetDateTime.now().minusDays(90);

        ArgumentCaptor<OffsetDateTime> commentCutoff = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(commentRepository).purgeIpHashOlderThan(commentCutoff.capture());
        assertThat(commentCutoff.getValue()).isBetween(before, after);

        ArgumentCaptor<OffsetDateTime> anonUserCutoff = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(anonUserRepository).purgeIpHashOlderThan(anonUserCutoff.capture());
        assertThat(anonUserCutoff.getValue()).isBetween(before, after);
    }
}
