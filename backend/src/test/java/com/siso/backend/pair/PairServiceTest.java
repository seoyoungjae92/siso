package com.siso.backend.pair;

import com.siso.backend.abuse.SpikeDetector;
import com.siso.backend.abuse.TrustScoreService;
import com.siso.backend.anon.AnonUser;
import com.siso.backend.anon.AnonUserRepository;
import com.siso.backend.anon.IpHasher;
import com.siso.backend.comment.CommentRepository;
import com.siso.backend.ratelimit.RateLimiter;
import com.siso.backend.settings.AbuseSettings;
import com.siso.backend.settings.AbuseSettingsRepository;
import com.siso.backend.settings.CrawlSettingsRepository;
import com.siso.backend.settings.ElectionSettings;
import com.siso.backend.settings.ElectionSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PairServiceTest {

    private static final UUID ANON_A = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private TopicPairRepository topicPairRepository;

    @Mock
    private VoteRepository voteRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private RateLimiter rateLimiter;

    @Mock
    private CrawlSettingsRepository crawlSettingsRepository;

    @Mock
    private AnonUserRepository anonUserRepository;

    @Mock
    private IpHasher ipHasher;

    @Mock
    private AbuseSettingsRepository abuseSettingsRepository;

    @Mock
    private TrustScoreService trustScoreService;

    @Mock
    private SpikeDetector spikeDetector;

    @Mock
    private ElectionSettingsRepository electionSettingsRepository;

    private PairService newService() {
        return new PairService(
                topicPairRepository,
                voteRepository,
                commentRepository,
                rateLimiter,
                crawlSettingsRepository,
                anonUserRepository,
                ipHasher,
                abuseSettingsRepository,
                trustScoreService,
                spikeDetector,
                electionSettingsRepository);
    }

    @BeforeEach
    void stubElectionModeDisabledByDefault() {
        stubElectionMode(false);
    }

    private void stubElectionMode(boolean enabled) {
        ElectionSettings settings = mock(ElectionSettings.class);
        Mockito.lenient().when(settings.isEnabled()).thenReturn(enabled);
        Mockito.lenient().when(electionSettingsRepository.findById((short) 1)).thenReturn(Optional.of(settings));
    }

    // getPairs/getFeaturedPair는 display_window_days와 election 모드를
    // 하나의 쿼리로 같이 조회한다(2026-08-12, DB 왕복 절약) — 두 값을
    // 같이 스텁한다.
    private void stubDisplayWindowAndElectionMode(int days, boolean electionEnabled) {
        CrawlSettingsRepository.DisplayWindowAndElectionMode settings =
                mock(CrawlSettingsRepository.DisplayWindowAndElectionMode.class);
        Mockito.lenient().when(settings.getDisplayWindowDays()).thenReturn(days);
        Mockito.lenient().when(settings.getElectionEnabled()).thenReturn(electionEnabled);
        when(crawlSettingsRepository.findDisplayWindowAndElectionMode()).thenReturn(settings);
    }

    private void stubAbuseSettings() {
        AbuseSettings settings = mock(AbuseSettings.class);
        Mockito.lenient().when(settings.getSpikeVoteThreshold()).thenReturn(30);
        Mockito.lenient().when(settings.getSpikeWindowMinutes()).thenReturn(10);
        Mockito.lenient().when(settings.getMultiAccountClusterSize()).thenReturn(4);
        Mockito.lenient().when(settings.getMultiAccountTrustPenaltyMultiplier()).thenReturn(0.3f);
        Mockito.lenient().when(settings.getTrustMaturityHours()).thenReturn(72);
        Mockito.lenient().when(settings.getTrustMinWeight()).thenReturn(0.3f);
        Mockito.lenient().when(abuseSettingsRepository.findById((short) 1)).thenReturn(Optional.of(settings));
    }

    @Test
    void getPairs_queriesActiveStatusWithSynthesizedTitleWithinDisplayWindow() {
        stubDisplayWindowAndElectionMode(7, false);
        PairService pairService = newService();
        Pageable pageable = PageRequest.of(0, 20);
        when(topicPairRepository.findByStatusAndTitleIsNotNullAndCreatedAtAfterOrderByEngagement(
                        eq("active"), any(OffsetDateTime.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        Page<TopicPairDto> result = pairService.getPairs(pageable);

        verify(topicPairRepository)
                .findByStatusAndTitleIsNotNullAndCreatedAtAfterOrderByEngagement(eq("active"), any(OffsetDateTime.class), eq(pageable));
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void getPairs_mapsTopicPairToDto() {
        stubDisplayWindowAndElectionMode(7, false);
        PairService pairService = newService();
        Pageable pageable = PageRequest.of(0, 20);

        TopicPair pair = new TopicPair();
        ReflectionTestUtils.setField(pair, "id", 100L);
        ReflectionTestUtils.setField(pair, "title", "합성된 주제 제목");
        ReflectionTestUtils.setField(pair, "leftStance", "좌 입장 요약");
        ReflectionTestUtils.setField(pair, "rightStance", "우 입장 요약");
        ReflectionTestUtils.setField(pair, "createdAt", OffsetDateTime.parse("2026-07-22T00:00:00Z"));

        when(topicPairRepository.findByStatusAndTitleIsNotNullAndCreatedAtAfterOrderByEngagement(
                        eq("active"), any(OffsetDateTime.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(pair)));

        when(voteRepository.countByPairIdsGroupByStance(List.of(100L))).thenReturn(List.of());

        Page<TopicPairDto> result = pairService.getPairs(pageable);

        assertThat(result.getContent()).hasSize(1);
        TopicPairDto dto = result.getContent().get(0);
        assertThat(dto.id()).isEqualTo(100L);
        assertThat(dto.title()).isEqualTo("합성된 주제 제목");
        assertThat(dto.leftStance()).isEqualTo("좌 입장 요약");
        assertThat(dto.rightStance()).isEqualTo("우 입장 요약");
        assertThat(dto.leftVotes()).isEqualTo(0.0);
        assertThat(dto.rightVotes()).isEqualTo(0.0);
        assertThat(dto.neutralVotes()).isEqualTo(0.0);
    }

    @Test
    void getPairs_includesVoteTallyPerPair() {
        stubDisplayWindowAndElectionMode(7, false);
        PairService pairService = newService();
        Pageable pageable = PageRequest.of(0, 20);

        TopicPair pairA = mock(TopicPair.class);
        when(pairA.getId()).thenReturn(100L);
        TopicPair pairB = mock(TopicPair.class);
        when(pairB.getId()).thenReturn(200L);

        when(topicPairRepository.findByStatusAndTitleIsNotNullAndCreatedAtAfterOrderByEngagement(
                        eq("active"), any(OffsetDateTime.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(pairA, pairB)));

        VoteRepository.StanceCountByPair rowA = mock(VoteRepository.StanceCountByPair.class);
        when(rowA.getPairId()).thenReturn(100L);
        when(rowA.getStance()).thenReturn("left");
        when(rowA.getTotal()).thenReturn(2.0);

        VoteRepository.StanceCountByPair rowB = mock(VoteRepository.StanceCountByPair.class);
        when(rowB.getPairId()).thenReturn(200L);
        when(rowB.getStance()).thenReturn("right");
        when(rowB.getTotal()).thenReturn(1.0);

        when(voteRepository.countByPairIdsGroupByStance(List.of(100L, 200L))).thenReturn(List.of(rowA, rowB));

        Page<TopicPairDto> result = pairService.getPairs(pageable);

        TopicPairDto dtoA = result.getContent().get(0);
        assertThat(dtoA.leftVotes()).isEqualTo(2.0);
        assertThat(dtoA.rightVotes()).isEqualTo(0.0);
        assertThat(dtoA.voteCount()).isEqualTo(2L);

        TopicPairDto dtoB = result.getContent().get(1);
        assertThat(dtoB.rightVotes()).isEqualTo(1.0);
        assertThat(dtoB.leftVotes()).isEqualTo(0.0);
        assertThat(dtoB.voteCount()).isEqualTo(1L);
    }

    @Test
    void getPairs_electionModeEnabled_hidesVoteTallyEvenWhenVotesExist() {
        stubDisplayWindowAndElectionMode(7, true);
        PairService pairService = newService();
        Pageable pageable = PageRequest.of(0, 20);

        TopicPair pair = mock(TopicPair.class);
        when(pair.getId()).thenReturn(100L);
        when(topicPairRepository.findByStatusAndTitleIsNotNullAndCreatedAtAfterOrderByEngagement(
                        eq("active"), any(OffsetDateTime.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(pair)));

        Page<TopicPairDto> result = pairService.getPairs(pageable);

        assertThat(result.getContent().get(0).leftVotes()).isEqualTo(0.0);
        assertThat(result.getContent().get(0).rightVotes()).isEqualTo(0.0);
        assertThat(result.getContent().get(0).neutralVotes()).isEqualTo(0.0);
        assertThat(result.getContent().get(0).voteCount()).isEqualTo(0L);
        verify(voteRepository, Mockito.never()).countByPairIdsGroupByStance(any());
    }

    @Test
    void getPairs_includesVisibleCommentCountPerPair() {
        stubDisplayWindowAndElectionMode(7, false);
        PairService pairService = newService();
        Pageable pageable = PageRequest.of(0, 20);

        TopicPair pairA = mock(TopicPair.class);
        when(pairA.getId()).thenReturn(100L);
        TopicPair pairB = mock(TopicPair.class);
        when(pairB.getId()).thenReturn(200L);

        when(topicPairRepository.findByStatusAndTitleIsNotNullAndCreatedAtAfterOrderByEngagement(
                        eq("active"), any(OffsetDateTime.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(pairA, pairB)));
        when(voteRepository.countByPairIdsGroupByStance(List.of(100L, 200L))).thenReturn(List.of());

        CommentRepository.CommentCountByPair countA = mock(CommentRepository.CommentCountByPair.class);
        when(countA.getPairId()).thenReturn(100L);
        when(countA.getTotal()).thenReturn(3L);
        when(commentRepository.countVisibleByPairIds(List.of(100L, 200L))).thenReturn(List.of(countA));

        Page<TopicPairDto> result = pairService.getPairs(pageable);

        assertThat(result.getContent().get(0).commentCount()).isEqualTo(3L);
        assertThat(result.getContent().get(1).commentCount()).isEqualTo(0L);
    }

    @Test
    void getPairs_voteCountIsSumOfStanceCountsAcrossMultipleVoters() {
        // voteCount는 이제 진영별 raw count(countByPairIdsGroupByStance)를
        // 합산해서 만든다 — 별도의 총계 쿼리가 없다(2026-08-12, DB 왕복
        // 절약을 위해 통합).
        stubDisplayWindowAndElectionMode(7, false);
        PairService pairService = newService();
        Pageable pageable = PageRequest.of(0, 20);

        TopicPair pairA = mock(TopicPair.class);
        when(pairA.getId()).thenReturn(100L);
        TopicPair pairB = mock(TopicPair.class);
        when(pairB.getId()).thenReturn(200L);

        when(topicPairRepository.findByStatusAndTitleIsNotNullAndCreatedAtAfterOrderByEngagement(
                        eq("active"), any(OffsetDateTime.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(pairA, pairB)));

        VoteRepository.StanceCountByPair leftRow = mock(VoteRepository.StanceCountByPair.class);
        when(leftRow.getPairId()).thenReturn(100L);
        when(leftRow.getStance()).thenReturn("left");
        when(leftRow.getTotal()).thenReturn(1.0);

        VoteRepository.StanceCountByPair rightRow = mock(VoteRepository.StanceCountByPair.class);
        when(rightRow.getPairId()).thenReturn(100L);
        when(rightRow.getStance()).thenReturn("right");
        when(rightRow.getTotal()).thenReturn(1.0);

        when(voteRepository.countByPairIdsGroupByStance(List.of(100L, 200L))).thenReturn(List.of(leftRow, rightRow));

        Page<TopicPairDto> result = pairService.getPairs(pageable);

        assertThat(result.getContent().get(0).voteCount()).isEqualTo(2L);
        assertThat(result.getContent().get(1).voteCount()).isEqualTo(0L);
    }

    @Test
    void getPair_pendingSynthesis_returnsNotFound() {
        when(topicPairRepository.findByIdAndStatusAndTitleIsNotNull(1L, "active")).thenReturn(Optional.empty());
        PairService pairService = newService();

        assertThatThrownBy(() -> pairService.getPair(1L, null))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void getPair_returnsVoteTally() {
        TopicPair pair = mock(TopicPair.class);
        when(topicPairRepository.findByIdAndStatusAndTitleIsNotNull(1L, "active")).thenReturn(Optional.of(pair));

        VoteRepository.StanceCount leftCount = mock(VoteRepository.StanceCount.class);
        when(leftCount.getStance()).thenReturn("left");
        when(leftCount.getTotal()).thenReturn(2.0);
        when(voteRepository.countByPairIdGroupByStance(1L)).thenReturn(List.of(leftCount));
        when(voteRepository.findByPair_IdAndAnonId(1L, ANON_A)).thenReturn(Optional.empty());

        CommentRepository.CommentCountByPair count = mock(CommentRepository.CommentCountByPair.class);
        when(count.getTotal()).thenReturn(5L);
        when(commentRepository.countVisibleByPairIds(List.of(1L))).thenReturn(List.of(count));

        TopicPairDto dto = newService().getPair(1L, ANON_A);

        assertThat(dto.leftVotes()).isEqualTo(2.0);
        assertThat(dto.rightVotes()).isEqualTo(0.0);
        // voteCount는 진영별 raw count의 합으로 만들어진다(가중치 없음,
        // 2026-08-12 결정) — 별도 총계 쿼리 없이 이 결과에서 바로 나온다.
        assertThat(dto.voteCount()).isEqualTo(2L);
        assertThat(dto.commentCount()).isEqualTo(5L);
    }

    @Test
    void getPair_electionModeEnabled_hidesVoteTallyEvenWhenVotesExist() {
        stubElectionMode(true);
        TopicPair pair = mock(TopicPair.class);
        when(topicPairRepository.findByIdAndStatusAndTitleIsNotNull(1L, "active")).thenReturn(Optional.of(pair));
        when(voteRepository.findByPair_IdAndAnonId(1L, ANON_A)).thenReturn(Optional.empty());

        TopicPairDto dto = newService().getPair(1L, ANON_A);

        assertThat(dto.leftVotes()).isEqualTo(0.0);
        assertThat(dto.rightVotes()).isEqualTo(0.0);
        assertThat(dto.neutralVotes()).isEqualTo(0.0);
        assertThat(dto.voteCount()).isEqualTo(0L);
        verify(voteRepository, Mockito.never()).countByPairIdGroupByStance(any());
    }

    @Test
    void getFeaturedPair_returnsTopEngagementPairMappedToDto() {
        stubDisplayWindowAndElectionMode(7, false);
        TopicPair pair = mock(TopicPair.class);
        when(pair.getId()).thenReturn(100L);
        when(topicPairRepository.findTopEngagementSince(any(OffsetDateTime.class), eq(PageRequest.of(0, 1))))
                .thenReturn(List.of(pair));

        VoteRepository.StanceCount leftCount = mock(VoteRepository.StanceCount.class);
        when(leftCount.getStance()).thenReturn("left");
        when(leftCount.getTotal()).thenReturn(4.0);
        VoteRepository.StanceCount rightCount = mock(VoteRepository.StanceCount.class);
        when(rightCount.getStance()).thenReturn("right");
        when(rightCount.getTotal()).thenReturn(2.0);
        when(voteRepository.countByPairIdGroupByStance(100L)).thenReturn(List.of(leftCount, rightCount));

        CommentRepository.CommentCountByPair count = mock(CommentRepository.CommentCountByPair.class);
        when(count.getTotal()).thenReturn(7L);
        when(commentRepository.countVisibleByPairIds(List.of(100L))).thenReturn(List.of(count));

        TopicPairDto dto = newService().getFeaturedPair();

        assertThat(dto.id()).isEqualTo(100L);
        assertThat(dto.leftVotes()).isEqualTo(4.0);
        assertThat(dto.voteCount()).isEqualTo(6L);
        assertThat(dto.commentCount()).isEqualTo(7L);
    }

    @Test
    void getFeaturedPair_noneWithinDisplayWindow_returnsNotFound() {
        stubDisplayWindowAndElectionMode(7, false);
        when(topicPairRepository.findTopEngagementSince(any(OffsetDateTime.class), eq(PageRequest.of(0, 1))))
                .thenReturn(List.of());

        assertThatThrownBy(() -> newService().getFeaturedPair())
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void vote_withNoExistingVote_createsOneAndRecordsAnonUserVote() {
        PairService pairService = newService();
        when(topicPairRepository.existsById(1L)).thenReturn(true);
        stubAbuseSettings();
        when(voteRepository.findByPair_IdAndAnonId(1L, ANON_A)).thenReturn(Optional.empty());
        when(topicPairRepository.getReferenceById(1L)).thenReturn(Mockito.mock(TopicPair.class));
        when(ipHasher.hash("127.0.0.1")).thenReturn("hashed-ip");
        when(anonUserRepository.findById(ANON_A)).thenReturn(Optional.empty());

        pairService.vote(1L, ANON_A, "127.0.0.1", "left");

        verify(voteRepository).save(any(Vote.class));

        ArgumentCaptor<AnonUser> captor = ArgumentCaptor.forClass(AnonUser.class);
        verify(anonUserRepository).save(captor.capture());
        assertThat(captor.getValue().getVoteCount()).isEqualTo(1);

        verify(spikeDetector).recordVoteAndCheck(1L, 30, 10);
        verify(trustScoreService)
                .recalculateForIpCluster(eq("hashed-ip"), any(OffsetDateTime.class), eq(4), eq(0.3f), eq(72), eq(0.3f));
    }

    @Test
    void vote_withExistingVote_updatesStanceInPlaceWithoutDoubleCountingVote() {
        PairService pairService = newService();
        TopicPair pair = Mockito.mock(TopicPair.class);
        Vote existing = new Vote(pair, ANON_A, "left", OffsetDateTime.now());
        when(topicPairRepository.existsById(1L)).thenReturn(true);
        stubAbuseSettings();
        when(voteRepository.findByPair_IdAndAnonId(1L, ANON_A)).thenReturn(Optional.of(existing));
        when(ipHasher.hash("127.0.0.1")).thenReturn("hashed-ip");

        pairService.vote(1L, ANON_A, "127.0.0.1", "right");

        assertThat(existing.getStance()).isEqualTo("right");
        verify(voteRepository, Mockito.never()).save(any(Vote.class));
        verify(anonUserRepository, Mockito.never()).save(any(AnonUser.class));
        verify(trustScoreService, Mockito.never()).recalculateForIpCluster(
                Mockito.anyString(), any(), Mockito.anyInt(), Mockito.anyFloat(), Mockito.anyInt(), Mockito.anyFloat());
        // 급증 탐지는 재투표(입장 변경)여도 여전히 호출됨 — 전체 투표 활동량을 본다
        verify(spikeDetector).recordVoteAndCheck(1L, 30, 10);
    }

    @Test
    void vote_invalidStance_isRejected() {
        PairService pairService = newService();

        assertThatThrownBy(() -> pairService.vote(1L, ANON_A, "127.0.0.1", "up"))
                .isInstanceOf(ResponseStatusException.class);
    }
}
