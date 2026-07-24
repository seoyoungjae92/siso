package com.siso.backend.pair;

import com.siso.backend.abuse.SpikeDetector;
import com.siso.backend.abuse.TrustScoreService;
import com.siso.backend.anon.AnonUser;
import com.siso.backend.anon.AnonUserRepository;
import com.siso.backend.anon.IpHasher;
import com.siso.backend.ratelimit.RateLimiter;
import com.siso.backend.settings.AbuseSettings;
import com.siso.backend.settings.AbuseSettingsRepository;
import com.siso.backend.settings.CrawlSettings;
import com.siso.backend.settings.CrawlSettingsRepository;
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

    private PairService newService() {
        return new PairService(
                topicPairRepository,
                voteRepository,
                rateLimiter,
                crawlSettingsRepository,
                anonUserRepository,
                ipHasher,
                abuseSettingsRepository,
                trustScoreService,
                spikeDetector);
    }

    private void stubDisplayWindowDays(int days) {
        CrawlSettings settings = mock(CrawlSettings.class);
        when(settings.getDisplayWindowDays()).thenReturn(days);
        when(crawlSettingsRepository.findById((short) 1)).thenReturn(Optional.of(settings));
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
        stubDisplayWindowDays(7);
        PairService pairService = newService();
        Pageable pageable = PageRequest.of(0, 20);
        when(topicPairRepository.findByStatusAndTitleIsNotNullAndCreatedAtAfter(
                        eq("active"), any(OffsetDateTime.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        Page<TopicPairDto> result = pairService.getPairs(pageable);

        verify(topicPairRepository)
                .findByStatusAndTitleIsNotNullAndCreatedAtAfter(eq("active"), any(OffsetDateTime.class), eq(pageable));
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void getPairs_mapsTopicPairToDto() {
        stubDisplayWindowDays(7);
        PairService pairService = newService();
        Pageable pageable = PageRequest.of(0, 20);

        TopicPair pair = new TopicPair();
        ReflectionTestUtils.setField(pair, "id", 100L);
        ReflectionTestUtils.setField(pair, "title", "합성된 주제 제목");
        ReflectionTestUtils.setField(pair, "leftStance", "좌 입장 요약");
        ReflectionTestUtils.setField(pair, "rightStance", "우 입장 요약");
        ReflectionTestUtils.setField(pair, "createdAt", OffsetDateTime.parse("2026-07-22T00:00:00Z"));

        when(topicPairRepository.findByStatusAndTitleIsNotNullAndCreatedAtAfter(
                        eq("active"), any(OffsetDateTime.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(pair)));

        Page<TopicPairDto> result = pairService.getPairs(pageable);

        assertThat(result.getContent()).hasSize(1);
        TopicPairDto dto = result.getContent().get(0);
        assertThat(dto.id()).isEqualTo(100L);
        assertThat(dto.title()).isEqualTo("합성된 주제 제목");
        assertThat(dto.leftStance()).isEqualTo("좌 입장 요약");
        assertThat(dto.rightStance()).isEqualTo("우 입장 요약");
    }

    @Test
    void getPair_pendingSynthesis_returnsNotFound() {
        when(topicPairRepository.findByIdAndStatusAndTitleIsNotNull(1L, "active")).thenReturn(Optional.empty());
        PairService pairService = newService();

        assertThatThrownBy(() -> pairService.getPair(1L, null))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void getPair_returnsTrustWeightedVoteTally() {
        TopicPair pair = mock(TopicPair.class);
        when(topicPairRepository.findByIdAndStatusAndTitleIsNotNull(1L, "active")).thenReturn(Optional.of(pair));

        VoteRepository.WeightedStanceCount leftCount = mock(VoteRepository.WeightedStanceCount.class);
        when(leftCount.getStance()).thenReturn("left");
        when(leftCount.getTotal()).thenReturn(1.8);
        when(voteRepository.sumWeightedByPairIdGroupByStance(1L)).thenReturn(List.of(leftCount));
        when(voteRepository.findByPair_IdAndAnonId(1L, ANON_A)).thenReturn(Optional.empty());

        TopicPairDto dto = newService().getPair(1L, ANON_A);

        assertThat(dto.leftVotes()).isEqualTo(1.8);
        assertThat(dto.rightVotes()).isEqualTo(0.0);
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
