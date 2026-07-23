package com.siso.backend.admin;

import com.siso.backend.source.CrawlType;
import com.siso.backend.source.Side;
import com.siso.backend.source.Source;
import com.siso.backend.source.SourceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class AdminSourceService {

    private final SourceRepository sourceRepository;

    public AdminSourceService(SourceRepository sourceRepository) {
        this.sourceRepository = sourceRepository;
    }

    @Transactional(readOnly = true)
    public List<SourceDto> list() {
        return sourceRepository.findAllByOrderByIdAsc().stream().map(this::toDto).toList();
    }

    @Transactional
    public SourceDto create(SourceRequest request) {
        Source source = new Source(
                request.name(),
                Side.fromValue(request.side()),
                request.baseUrl(),
                request.feedUrl(),
                CrawlType.fromValue(request.crawlType()),
                OffsetDateTime.now());
        sourceRepository.save(source);
        return toDto(source);
    }

    @Transactional
    public SourceDto update(Long id, SourceRequest request) {
        Source source = findOrThrow(id);
        source.update(
                request.name(),
                Side.fromValue(request.side()),
                request.baseUrl(),
                request.feedUrl(),
                CrawlType.fromValue(request.crawlType()));
        return toDto(source);
    }

    @Transactional
    public SourceDto toggle(Long id) {
        Source source = findOrThrow(id);
        source.toggle();
        return toDto(source);
    }

    private Source findOrThrow(Long id) {
        return sourceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "source not found"));
    }

    private SourceDto toDto(Source source) {
        return new SourceDto(
                source.getId(),
                source.getName(),
                source.getSide().value(),
                source.getBaseUrl(),
                source.getFeedUrl(),
                source.getCrawlType().value(),
                source.isEnabled(),
                source.getCreatedAt());
    }
}
