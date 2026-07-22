package com.siso.backend.pair;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
