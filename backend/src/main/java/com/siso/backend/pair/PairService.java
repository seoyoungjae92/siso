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
import com.siso.backend.settings.ElectionSettingsRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class PairService {

    private static final String ACTIVE_STATUS = "active";
    private static final Set<String> STANCES = Set.of("left", "right", "neutral");
    private static final short SETTINGS_ID = 1;

    private final TopicPairRepository topicPairRepository;
    private final VoteRepository voteRepository;
    private final CommentRepository commentRepository;
    private final RateLimiter rateLimiter;
    private final CrawlSettingsRepository crawlSettingsRepository;
    private final AnonUserRepository anonUserRepository;
    private final IpHasher ipHasher;
    private final AbuseSettingsRepository abuseSettingsRepository;
    private final TrustScoreService trustScoreService;
    private final SpikeDetector spikeDetector;
    private final ElectionSettingsRepository electionSettingsRepository;

    public PairService(
            TopicPairRepository topicPairRepository,
            VoteRepository voteRepository,
            CommentRepository commentRepository,
            RateLimiter rateLimiter,
            CrawlSettingsRepository crawlSettingsRepository,
            AnonUserRepository anonUserRepository,
            IpHasher ipHasher,
            AbuseSettingsRepository abuseSettingsRepository,
            TrustScoreService trustScoreService,
            SpikeDetector spikeDetector,
            ElectionSettingsRepository electionSettingsRepository) {
        this.topicPairRepository = topicPairRepository;
        this.voteRepository = voteRepository;
        this.commentRepository = commentRepository;
        this.rateLimiter = rateLimiter;
        this.crawlSettingsRepository = crawlSettingsRepository;
        this.anonUserRepository = anonUserRepository;
        this.ipHasher = ipHasher;
        this.abuseSettingsRepository = abuseSettingsRepository;
        this.trustScoreService = trustScoreService;
        this.spikeDetector = spikeDetector;
        this.electionSettingsRepository = electionSettingsRepository;
    }

    @Transactional(readOnly = true)
    public Page<TopicPairDto> getPairs(Pageable pageable) {
        int displayWindowDays = crawlSettingsRepository.findById(SETTINGS_ID).orElseThrow().getDisplayWindowDays();
        OffsetDateTime since = OffsetDateTime.now().minusDays(displayWindowDays);
        Page<TopicPair> pairs = topicPairRepository.findByStatusAndTitleIsNotNullAndCreatedAtAfter(
                ACTIVE_STATUS, since, pageable);

        // D10: 선거 모드 중엔 공직선거법상 여론조사 결과 공표 리스크를 피하기
        // 위해 API 응답 자체에서 득표 집계를 감춘다(프론트 렌더링만 가리는 걸로는
        // curl/스크립트로 그대로 노출됨 — 실측으로 확인된 갭).
        boolean electionMode = electionSettingsRepository.findById(SETTINGS_ID).orElseThrow().isEnabled();

        List<Long> pairIds = pairs.getContent().stream().map(TopicPair::getId).toList();
        Map<Long, Map<String, Double>> talliesByPairId = new HashMap<>();
        Map<Long, Long> commentCountsByPairId = new HashMap<>();
        if (!pairIds.isEmpty()) {
            if (!electionMode) {
                for (VoteRepository.WeightedStanceCountByPair row : voteRepository.sumWeightedByPairIdsGroupByStance(pairIds)) {
                    talliesByPairId
                            .computeIfAbsent(row.getPairId(), key -> new HashMap<>())
                            .put(row.getStance(), row.getTotal());
                }
            }
            for (CommentRepository.CommentCountByPair row : commentRepository.countVisibleByPairIds(pairIds)) {
                commentCountsByPairId.put(row.getPairId(), row.getTotal());
            }
        }

        return pairs.map(pair -> {
            Map<String, Double> tally = talliesByPairId.getOrDefault(pair.getId(), Map.of());
            return TopicPairDto.from(
                    pair,
                    tally.getOrDefault("left", 0.0),
                    tally.getOrDefault("right", 0.0),
                    tally.getOrDefault("neutral", 0.0),
                    commentCountsByPairId.getOrDefault(pair.getId(), 0L),
                    null);
        });
    }

    // "오늘의 링" — 최근성이 아니라 (투표+댓글) 참여도가 가장 높은 주제 1건.
    // getPairs와 같은 display_window_days 창 안에서, 뉴스레터용
    // findTopEngagementSince와 동일한 단순 랭킹 기준을 그대로 재사용한다.
    @Transactional(readOnly = true)
    public TopicPairDto getFeaturedPair() {
        int displayWindowDays = crawlSettingsRepository.findById(SETTINGS_ID).orElseThrow().getDisplayWindowDays();
        OffsetDateTime since = OffsetDateTime.now().minusDays(displayWindowDays);
        List<TopicPair> top = topicPairRepository.findTopEngagementSince(since, PageRequest.of(0, 1));
        if (top.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "no featured pair");
        }
        TopicPair pair = top.get(0);

        boolean electionMode = electionSettingsRepository.findById(SETTINGS_ID).orElseThrow().isEnabled();
        Map<String, Double> tally = new HashMap<>();
        if (!electionMode) {
            for (VoteRepository.WeightedStanceCount row : voteRepository.sumWeightedByPairIdGroupByStance(pair.getId())) {
                tally.put(row.getStance(), row.getTotal());
            }
        }

        long commentCount = commentRepository.countVisibleByPairIds(List.of(pair.getId())).stream()
                .findFirst()
                .map(CommentRepository.CommentCountByPair::getTotal)
                .orElse(0L);

        return TopicPairDto.from(
                pair,
                tally.getOrDefault("left", 0.0),
                tally.getOrDefault("right", 0.0),
                tally.getOrDefault("neutral", 0.0),
                commentCount,
                null);
    }

    @Transactional(readOnly = true)
    public TopicPairDto getPair(Long id, UUID viewerAnonId) {
        TopicPair pair = topicPairRepository.findByIdAndStatusAndTitleIsNotNull(id, ACTIVE_STATUS)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "pair not found"));

        boolean electionMode = electionSettingsRepository.findById(SETTINGS_ID).orElseThrow().isEnabled();
        Map<String, Double> tally = new HashMap<>();
        if (!electionMode) {
            for (VoteRepository.WeightedStanceCount row : voteRepository.sumWeightedByPairIdGroupByStance(id)) {
                tally.put(row.getStance(), row.getTotal());
            }
        }

        String myStance = viewerAnonId == null
                ? null
                : voteRepository.findByPair_IdAndAnonId(id, viewerAnonId).map(Vote::getStance).orElse(null);

        long commentCount = commentRepository.countVisibleByPairIds(List.of(id)).stream()
                .findFirst()
                .map(CommentRepository.CommentCountByPair::getTotal)
                .orElse(0L);

        return TopicPairDto.from(
                pair,
                tally.getOrDefault("left", 0.0),
                tally.getOrDefault("right", 0.0),
                tally.getOrDefault("neutral", 0.0),
                commentCount,
                myStance);
    }

    @Transactional
    public void vote(Long pairId, UUID anonId, String remoteAddr, String stance) {
        if (!STANCES.contains(stance)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "stance must be left, right, or neutral");
        }

        rateLimiter.checkOrThrow("vote", anonId);

        if (!topicPairRepository.existsById(pairId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "pair not found");
        }

        AbuseSettings abuseSettings = abuseSettingsRepository.findById(SETTINGS_ID).orElseThrow();
        spikeDetector.recordVoteAndCheck(
                pairId, abuseSettings.getSpikeVoteThreshold(), abuseSettings.getSpikeWindowMinutes());

        OffsetDateTime now = OffsetDateTime.now();
        String ipHash = ipHasher.hash(remoteAddr);

        voteRepository.findByPair_IdAndAnonId(pairId, anonId)
                .ifPresentOrElse(
                        vote -> vote.update(stance, now),
                        () -> {
                            voteRepository.save(
                                    new Vote(topicPairRepository.getReferenceById(pairId), anonId, stance, now));

                            // 기존 투표를 입장만 바꾸는 건 새 투표가 아니므로(위 update
                            // 분기) 여기(진짜 신규 투표일 때만) vote_count를 올린다 —
                            // 안 그러면 입장을 왔다갔다 바꿔서 카운트를 무한정 farming
                            // 할 수 있음.
                            AnonUser anonUser = anonUserRepository.findById(anonId)
                                    .orElseGet(() -> new AnonUser(anonId, now, ipHash));
                            anonUser.recordVote(now, ipHash);
                            anonUserRepository.save(anonUser);

                            trustScoreService.recalculateForIpCluster(
                                    ipHash,
                                    now,
                                    abuseSettings.getMultiAccountClusterSize(),
                                    abuseSettings.getMultiAccountTrustPenaltyMultiplier(),
                                    abuseSettings.getTrustMaturityHours(),
                                    abuseSettings.getTrustMinWeight());
                        });
    }
}
