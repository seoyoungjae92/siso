package com.siso.backend.post;

import com.siso.backend.settings.CrawlSettingsRepository;
import com.siso.backend.source.Side;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class PostService {

    private static final short SETTINGS_ID = 1;

    private final PostRepository postRepository;
    private final CrawlSettingsRepository crawlSettingsRepository;

    public PostService(PostRepository postRepository, CrawlSettingsRepository crawlSettingsRepository) {
        this.postRepository = postRepository;
        this.crawlSettingsRepository = crawlSettingsRepository;
    }

    @Transactional(readOnly = true)
    public Page<PostSummaryDto> getFeed(Side side, Pageable pageable) {
        int displayWindowDays = crawlSettingsRepository.findById(SETTINGS_ID).orElseThrow().getDisplayWindowDays();
        OffsetDateTime since = OffsetDateTime.now().minusDays(displayWindowDays);
        return postRepository.findBySource_SideAndSource_EnabledTrueAndCollectedAtAfter(side, since, pageable)
                .map(PostSummaryDto::from);
    }
}
