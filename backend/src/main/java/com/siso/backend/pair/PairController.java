package com.siso.backend.pair;

import com.siso.backend.anon.AnonIdHeader;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
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
    public TopicPairDto getPair(
            @PathVariable Long id,
            @RequestHeader(value = "X-Anon-Id", required = false) String anonId) {
        return pairService.getPair(id, AnonIdHeader.parse(anonId, false));
    }

    @PostMapping("/api/pairs/{pairId}/votes")
    public void vote(
            @PathVariable Long pairId,
            @RequestHeader(value = "X-Anon-Id", required = false) String anonId,
            @RequestBody VoteCreateRequest request) {
        pairService.vote(pairId, AnonIdHeader.parse(anonId, true), request.stance());
    }
}
