package com.siso.backend.post;

import com.siso.backend.settings.CrawlSettings;
import com.siso.backend.settings.CrawlSettingsRepository;
import com.siso.backend.source.Side;
import com.siso.backend.source.Source;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private CrawlSettingsRepository crawlSettingsRepository;

    private void stubDisplayWindowDays(int days) {
        CrawlSettings settings = mock(CrawlSettings.class);
        when(settings.getDisplayWindowDays()).thenReturn(days);
        when(crawlSettingsRepository.findById((short) 1)).thenReturn(Optional.of(settings));
    }

    @Test
    void getFeed_queriesByRequestedSideWithinDisplayWindow() {
        stubDisplayWindowDays(7);
        PostService postService = new PostService(postRepository, crawlSettingsRepository);
        Pageable pageable = PageRequest.of(0, 20);
        when(postRepository.findFeed(eq(Side.LEFT), any(OffsetDateTime.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        Page<PostSummaryDto> result = postService.getFeed(Side.LEFT, pageable);

        verify(postRepository).findFeed(eq(Side.LEFT), any(OffsetDateTime.class), eq(pageable));
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void getFeed_mapsPostEntityToSummaryDto() {
        stubDisplayWindowDays(7);
        PostService postService = new PostService(postRepository, crawlSettingsRepository);
        Pageable pageable = PageRequest.of(0, 20);

        Source source = org.mockito.Mockito.mock(Source.class);
        when(source.getName()).thenReturn("루리웹");

        Post post = new Post();
        ReflectionTestUtils.setField(post, "id", 10L);
        ReflectionTestUtils.setField(post, "source", source);
        ReflectionTestUtils.setField(post, "title", "제목");
        ReflectionTestUtils.setField(post, "summary", "요약");
        ReflectionTestUtils.setField(post, "originUrl", "https://example.test/1");
        ReflectionTestUtils.setField(post, "collectedAt", OffsetDateTime.parse("2026-07-21T00:00:00Z"));

        when(postRepository.findFeed(eq(Side.RIGHT), any(OffsetDateTime.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(post)));

        Page<PostSummaryDto> result = postService.getFeed(Side.RIGHT, pageable);

        assertThat(result.getContent()).hasSize(1);
        PostSummaryDto dto = result.getContent().get(0);
        assertThat(dto.id()).isEqualTo(10L);
        assertThat(dto.title()).isEqualTo("제목");
        assertThat(dto.sourceName()).isEqualTo("루리웹");
        assertThat(dto.originUrl()).isEqualTo("https://example.test/1");
    }
}
