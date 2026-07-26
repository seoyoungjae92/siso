package com.siso.backend.admin;

import com.siso.backend.newsletter.NewsletterSendService;
import com.siso.backend.newsletter.NewsletterSubscriberRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/newsletter")
public class AdminNewsletterController {

    private final NewsletterSubscriberRepository newsletterSubscriberRepository;
    private final NewsletterSendService newsletterSendService;

    public AdminNewsletterController(
            NewsletterSubscriberRepository newsletterSubscriberRepository,
            NewsletterSendService newsletterSendService) {
        this.newsletterSubscriberRepository = newsletterSubscriberRepository;
        this.newsletterSendService = newsletterSendService;
    }

    @GetMapping("/stats")
    public NewsletterStatsDto stats() {
        return new NewsletterStatsDto(
                newsletterSubscriberRepository.countByStatus("pending"),
                newsletterSubscriberRepository.countByStatus("confirmed"),
                newsletterSubscriberRepository.countByStatus("unsubscribed"));
    }

    @PostMapping("/send-now")
    public int sendNow() {
        return newsletterSendService.sendWeeklyDigest();
    }
}
