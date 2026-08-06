package com.siso.backend.pair;

import com.siso.backend.anon.AnonIdHeader;
import com.siso.backend.anon.AnonIdSigner;
import com.siso.backend.anon.ClientIp;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
    private final AnonIdSigner anonIdSigner;

    public PairController(PairService pairService, AnonIdSigner anonIdSigner) {
        this.pairService = pairService;
        this.anonIdSigner = anonIdSigner;
    }

    @GetMapping("/api/pairs")
    public Page<TopicPairDto> getPairs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return pairService.getPairs(pageable);
    }

    @GetMapping("/api/pairs/featured")
    public TopicPairDto getFeaturedPair() {
        return pairService.getFeaturedPair();
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
            @RequestHeader(value = "X-Anon-Sig", required = false) String anonSig,
            @RequestBody VoteCreateRequest request,
            HttpServletRequest servletRequest) {
        pairService.vote(
                pairId,
                AnonIdHeader.parseAndVerify(anonId, anonSig, anonIdSigner),
                ClientIp.resolve(servletRequest),
                request.stance());
    }
}
