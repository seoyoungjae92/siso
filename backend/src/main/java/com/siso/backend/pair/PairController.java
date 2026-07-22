package com.siso.backend.pair;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PairController {

    private final PairService pairService;

    public PairController(PairService pairService) {
        this.pairService = pairService;
    }

    @GetMapping("/api/pairs")
    public Page<TopicPairDto> getPairs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return pairService.getPairs(pageable);
    }

    @GetMapping("/api/pairs/{id}")
    public TopicPairDto getPair(@PathVariable Long id) {
        return pairService.getPair(id);
    }
}
