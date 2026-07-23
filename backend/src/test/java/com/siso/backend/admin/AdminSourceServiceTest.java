package com.siso.backend.admin;

import com.siso.backend.source.Source;
import com.siso.backend.source.SourceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminSourceServiceTest {

    @Mock
    private SourceRepository sourceRepository;

    private AdminSourceService newService() {
        return new AdminSourceService(sourceRepository);
    }

    private SourceRequest validRequest() {
        return new SourceRequest("루리웹", "left", "https://bbs.ruliweb.com", "https://bbs.ruliweb.com/rss", "rss");
    }

    @Test
    void create_validRequest_savesAndReturnsDto() {
        SourceDto dto = newService().create(validRequest());

        assertThat(dto.name()).isEqualTo("루리웹");
        assertThat(dto.side()).isEqualTo("left");
        assertThat(dto.crawlType()).isEqualTo("rss");
        assertThat(dto.enabled()).isTrue();
    }

    @Test
    void create_invalidSide_isRejected() {
        SourceRequest request = new SourceRequest("루리웹", "up", "https://bbs.ruliweb.com", null, "rss");

        assertThatThrownBy(() -> newService().create(request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void create_invalidCrawlType_isRejected() {
        SourceRequest request = new SourceRequest("루리웹", "left", "https://bbs.ruliweb.com", null, "json");

        assertThatThrownBy(() -> newService().create(request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private Source sourceWithId(long id) {
        Source source = new Source(
                "일베", com.siso.backend.source.Side.RIGHT, "https://ilbe.com", null,
                com.siso.backend.source.CrawlType.HTML, OffsetDateTime.now());
        ReflectionTestUtils.setField(source, "id", id);
        return source;
    }

    @Test
    void update_existingSource_updatesFields() {
        Source source = sourceWithId(1L);
        when(sourceRepository.findById(1L)).thenReturn(Optional.of(source));

        SourceDto dto = newService().update(1L, validRequest());

        assertThat(dto.name()).isEqualTo("루리웹");
        assertThat(dto.side()).isEqualTo("left");
        assertThat(dto.crawlType()).isEqualTo("rss");
    }

    @Test
    void update_nonExistentId_isNotFound() {
        when(sourceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> newService().update(99L, validRequest()))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void toggle_flipsEnabled() {
        Source source = sourceWithId(1L);
        when(sourceRepository.findById(1L)).thenReturn(Optional.of(source));

        SourceDto first = newService().toggle(1L);
        assertThat(first.enabled()).isFalse();

        SourceDto second = newService().toggle(1L);
        assertThat(second.enabled()).isTrue();
    }

    @Test
    void toggle_nonExistentId_isNotFound() {
        when(sourceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> newService().toggle(99L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }
}
