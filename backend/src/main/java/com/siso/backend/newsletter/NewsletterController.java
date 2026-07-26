package com.siso.backend.newsletter;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 이메일 링크는 프론트 페이지(app/newsletter/confirm, /unsubscribe)를
 * 가리키고, 그 페이지가 서버에서 이 API를 호출한다 — GET으로 상태를
 * 바꾸는 걸 피하기 위해(이메일 클라이언트가 직접 POST를 못 보내므로).
 */
@RestController
@RequestMapping("/api/newsletter")
public class NewsletterController {

    private final NewsletterService newsletterService;

    public NewsletterController(NewsletterService newsletterService) {
        this.newsletterService = newsletterService;
    }

    @PostMapping("/subscribe")
    public void subscribe(@RequestBody NewsletterSubscribeRequest request) {
        newsletterService.subscribe(request.email());
    }

    @PostMapping("/confirm")
    public void confirm(@RequestBody NewsletterTokenRequest request) {
        newsletterService.confirm(request.token());
    }

    @PostMapping("/unsubscribe")
    public void unsubscribe(@RequestBody NewsletterTokenRequest request) {
        newsletterService.unsubscribe(request.token());
    }
}
