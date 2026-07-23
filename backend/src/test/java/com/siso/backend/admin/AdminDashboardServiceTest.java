package com.siso.backend.admin;

import com.siso.backend.anon.AnonUserRepository;
import com.siso.backend.comment.CommentRepository;
import com.siso.backend.comment.ReportRepository;
import com.siso.backend.pair.VoteRepository;
import com.siso.backend.post.Post;
import com.siso.backend.post.PostRepository;
import com.siso.backend.source.CrawlType;
import com.siso.backend.source.Side;
import com.siso.backend.source.Source;
import com.siso.backend.source.SourceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private VoteRepository voteRepository;

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private AnonUserRepository anonUserRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private SourceRepository sourceRepository;

    private AdminDashboardService newService() {
        return new AdminDashboardService(
                commentRepository, voteRepository, reportRepository, anonUserRepository, postRepository,
                sourceRepository);
    }

    private Source sourceWithId(long id, String name, Side side) {
        Source source = new Source(name, side, "https://example.com", null, CrawlType.RSS, OffsetDateTime.now());
        ReflectionTestUtils.setField(source, "id", id);
        return source;
    }

    @Test
    void getDashboard_aggregatesCountsAndSourceStats() {
        when(commentRepository.count()).thenReturn(42L);
        when(voteRepository.count()).thenReturn(17L);
        when(reportRepository.countByStatus("pending")).thenReturn(3L);
        when(reportRepository.count()).thenReturn(10L);
        when(anonUserRepository.countByFirstSeenAfter(any())).thenReturn(5L);
        when(anonUserRepository.countByLastSeenAfter(any())).thenReturn(8L);

        Source left = sourceWithId(1L, "루리웹", Side.LEFT);
        when(sourceRepository.findAllByOrderByIdAsc()).thenReturn(List.of(left));

        Post latest = mockPostWithCollectedAt(OffsetDateTime.now());
        when(postRepository.countBySource_Id(1L)).thenReturn(6L);
        when(postRepository.findFirstBySource_IdOrderByCollectedAtDesc(1L)).thenReturn(Optional.of(latest));

        DashboardDto dto = newService().getDashboard();

        assertThat(dto.totalComments()).isEqualTo(42L);
        assertThat(dto.totalVotes()).isEqualTo(17L);
        assertThat(dto.pendingReports()).isEqualTo(3L);
        assertThat(dto.totalReports()).isEqualTo(10L);
        assertThat(dto.newAnonUsersLast24h()).isEqualTo(5L);
        assertThat(dto.activeAnonUsersLast24h()).isEqualTo(8L);
        assertThat(dto.sourceStats()).hasSize(1);
        assertThat(dto.sourceStats().get(0).sourceName()).isEqualTo("루리웹");
        assertThat(dto.sourceStats().get(0).side()).isEqualTo("left");
        assertThat(dto.sourceStats().get(0).postCount()).isEqualTo(6L);
        assertThat(dto.sourceStats().get(0).lastCollectedAt()).isNotNull();
    }

    @Test
    void getDashboard_sourceWithNoPosts_lastCollectedAtIsNull() {
        when(commentRepository.count()).thenReturn(0L);
        when(voteRepository.count()).thenReturn(0L);
        when(reportRepository.countByStatus("pending")).thenReturn(0L);
        when(reportRepository.count()).thenReturn(0L);
        when(anonUserRepository.countByFirstSeenAfter(any())).thenReturn(0L);
        when(anonUserRepository.countByLastSeenAfter(any())).thenReturn(0L);

        Source right = sourceWithId(2L, "일베", Side.RIGHT);
        when(sourceRepository.findAllByOrderByIdAsc()).thenReturn(List.of(right));
        when(postRepository.countBySource_Id(2L)).thenReturn(0L);
        when(postRepository.findFirstBySource_IdOrderByCollectedAtDesc(2L)).thenReturn(Optional.empty());

        DashboardDto dto = newService().getDashboard();

        assertThat(dto.sourceStats().get(0).postCount()).isZero();
        assertThat(dto.sourceStats().get(0).lastCollectedAt()).isNull();
    }

    private Post mockPostWithCollectedAt(OffsetDateTime collectedAt) {
        Post post = org.mockito.Mockito.mock(Post.class);
        when(post.getCollectedAt()).thenReturn(collectedAt);
        return post;
    }
}
