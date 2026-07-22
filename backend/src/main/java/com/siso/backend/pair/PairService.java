package com.siso.backend.pair;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PairService {

    private static final String ACTIVE_STATUS = "active";

    private final TopicPairRepository topicPairRepository;

    public PairService(TopicPairRepository topicPairRepository) {
        this.topicPairRepository = topicPairRepository;
    }

    @Transactional(readOnly = true)
    public Page<TopicPairDto> getPairs(Pageable pageable) {
        return topicPairRepository.findByStatus(ACTIVE_STATUS, pageable)
                .map(TopicPairDto::from);
    }

    @Transactional(readOnly = true)
    public TopicPairDto getPair(Long id) {
        return topicPairRepository.findByIdAndStatus(id, ACTIVE_STATUS)
                .map(TopicPairDto::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "pair not found"));
    }
}
