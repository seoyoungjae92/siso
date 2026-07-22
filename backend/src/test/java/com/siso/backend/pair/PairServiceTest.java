package com.siso.backend.pair;

import com.siso.backend.post.Post;
import com.siso.backend.source.Source;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PairServiceTest {

    @Mock
    private TopicPairRepository topicPairRepository;

    @Test
    void getPairs_queriesActiveStatus() {
        PairService pairService = new PairService(topicPairRepository);
        Pageable pageable = PageRequest.of(0, 20);
        when(topicPairRepository.findByStatus(eq("active"), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        Page<TopicPairDto> result = pairService.getPairs(pageable);

        verify(topicPairRepository).findByStatus("active", pageable);
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void getPairs_mapsTopicPairToDto() {
        PairService pairService = new PairService(topicPairRepository);
        Pageable pageable = PageRequest.of(0, 20);

        Source leftSource = Mockito.mock(Source.class);
        when(leftSource.getName()).thenReturn("루리웹");
        Post leftPost = Mockito.mock(Post.class);
        when(leftPost.getId()).thenReturn(1L);
        when(leftPost.getTitle()).thenReturn("좌 제목");
        when(leftPost.getSummary()).thenReturn("좌 요약");
        when(leftPost.getOriginUrl()).thenReturn("https://example.test/left");
        when(leftPost.getSource()).thenReturn(leftSource);

        Source rightSource = Mockito.mock(Source.class);
        when(rightSource.getName()).thenReturn("일베");
        Post rightPost = Mockito.mock(Post.class);
        when(rightPost.getId()).thenReturn(2L);
        when(rightPost.getTitle()).thenReturn("우 제목");
        when(rightPost.getSummary()).thenReturn("우 요약");
        when(rightPost.getOriginUrl()).thenReturn("https://example.test/right");
        when(rightPost.getSource()).thenReturn(rightSource);

        TopicPair pair = new TopicPair();
        ReflectionTestUtils.setField(pair, "id", 100L);
        ReflectionTestUtils.setField(pair, "leftPost", leftPost);
        ReflectionTestUtils.setField(pair, "rightPost", rightPost);
        ReflectionTestUtils.setField(pair, "similarity", 0.62f);
        ReflectionTestUtils.setField(pair, "createdAt", OffsetDateTime.parse("2026-07-22T00:00:00Z"));

        when(topicPairRepository.findByStatus(eq("active"), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(pair)));

        Page<TopicPairDto> result = pairService.getPairs(pageable);

        assertThat(result.getContent()).hasSize(1);
        TopicPairDto dto = result.getContent().get(0);
        assertThat(dto.id()).isEqualTo(100L);
        assertThat(dto.similarity()).isEqualTo(0.62f);
        assertThat(dto.leftPost().sourceName()).isEqualTo("루리웹");
        assertThat(dto.rightPost().sourceName()).isEqualTo("일베");
    }
}
