package com.siso.backend.pair;

import com.siso.backend.abuse.SpikeDetector;
import com.siso.backend.abuse.TrustScoreService;
import com.siso.backend.anon.AnonUser;
import com.siso.backend.anon.AnonUserRepository;
import com.siso.backend.anon.IpHasher;
import com.siso.backend.ratelimit.RateLimiter;
import com.siso.backend.settings.AbuseSettings;
import com.siso.backend.settings.AbuseSettingsRepository;
import com.siso.backend.settings.CrawlSettingsRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.HashMap;
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
    private final RateLimiter rateLimiter;
    private final CrawlSettingsRepository crawlSettingsRepository;
    private final AnonUserRepository anonUserRepository;
    private final IpHasher ipHasher;
    private final AbuseSettingsRepository abuseSettingsRepository;
    private final TrustScoreService trustScoreService;
    private final SpikeDetector spikeDetector;

    public PairService(
            TopicPairRepository topicPairRepository,
            VoteRepository voteRepository,
            RateLimiter rateLimiter,
            CrawlSettingsRepository crawlSettingsRepository,
            AnonUserRepository anonUserRepository,
            IpHasher ipHasher,
            AbuseSettingsRepository abuseSettingsRepository,
            TrustScoreService trustScoreService,
            SpikeDetector spikeDetector) {
        this.topicPairRepository = topicPairRepository;
        this.voteRepository = voteRepository;
        this.rateLimiter = rateLimiter;
        this.crawlSettingsRepository = crawlSettingsRepository;
        this.anonUserRepository = anonUserRepository;
        this.ipHasher = ipHasher;
        this.abuseSettingsRepository = abuseSettingsRepository;
        this.trustScoreService = trustScoreService;
        this.spikeDetector = spikeDetector;
    }

    @Transactional(readOnly = true)
    public Page<TopicPairDto> getPairs(Pageable pageable) {
        int displayWindowDays = crawlSettingsRepository.findById(SETTINGS_ID).orElseThrow().getDisplayWindowDays();
        OffsetDateTime since = OffsetDateTime.now().minusDays(displayWindowDays);
        return topicPairRepository.findByStatusAndTitleIsNotNullAndCreatedAtAfter(ACTIVE_STATUS, since, pageable)
                .map(TopicPairDto::from);
    }

    @Transactional(readOnly = true)
    public TopicPairDto getPair(Long id, UUID viewerAnonId) {
        TopicPair pair = topicPairRepository.findByIdAndStatusAndTitleIsNotNull(id, ACTIVE_STATUS)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "pair not found"));

        Map<String, Double> tally = new HashMap<>();
        for (VoteRepository.WeightedStanceCount row : voteRepository.sumWeightedByPairIdGroupByStance(id)) {
            tally.put(row.getStance(), row.getTotal());
        }

        String myStance = viewerAnonId == null
                ? null
                : voteRepository.findByPair_IdAndAnonId(id, viewerAnonId).map(Vote::getStance).orElse(null);

        return TopicPairDto.from(
                pair,
                tally.getOrDefault("left", 0.0),
                tally.getOrDefault("right", 0.0),
                tally.getOrDefault("neutral", 0.0),
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
