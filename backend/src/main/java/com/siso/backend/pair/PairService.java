package com.siso.backend.pair;

import com.siso.backend.ratelimit.RateLimiter;
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

    private final TopicPairRepository topicPairRepository;
    private final VoteRepository voteRepository;
    private final RateLimiter rateLimiter;

    public PairService(
            TopicPairRepository topicPairRepository, VoteRepository voteRepository, RateLimiter rateLimiter) {
        this.topicPairRepository = topicPairRepository;
        this.voteRepository = voteRepository;
        this.rateLimiter = rateLimiter;
    }

    @Transactional(readOnly = true)
    public Page<TopicPairDto> getPairs(Pageable pageable) {
        return topicPairRepository.findByStatus(ACTIVE_STATUS, pageable)
                .map(TopicPairDto::from);
    }

    @Transactional(readOnly = true)
    public TopicPairDto getPair(Long id, UUID viewerAnonId) {
        TopicPair pair = topicPairRepository.findByIdAndStatus(id, ACTIVE_STATUS)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "pair not found"));

        Map<String, Long> tally = new HashMap<>();
        for (VoteRepository.StanceCount row : voteRepository.countByPairIdGroupByStance(id)) {
            tally.put(row.getStance(), row.getTotal());
        }

        String myStance = viewerAnonId == null
                ? null
                : voteRepository.findByPair_IdAndAnonId(id, viewerAnonId).map(Vote::getStance).orElse(null);

        return TopicPairDto.from(
                pair,
                tally.getOrDefault("left", 0L),
                tally.getOrDefault("right", 0L),
                tally.getOrDefault("neutral", 0L),
                myStance);
    }

    @Transactional
    public void vote(Long pairId, UUID anonId, String stance) {
        if (!STANCES.contains(stance)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "stance must be left, right, or neutral");
        }

        rateLimiter.checkOrThrow("vote", anonId);

        if (!topicPairRepository.existsById(pairId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "pair not found");
        }

        OffsetDateTime now = OffsetDateTime.now();
        voteRepository.findByPair_IdAndAnonId(pairId, anonId)
                .ifPresentOrElse(
                        vote -> vote.update(stance, now),
                        () -> voteRepository.save(
                                new Vote(topicPairRepository.getReferenceById(pairId), anonId, stance, now)));
    }
}
