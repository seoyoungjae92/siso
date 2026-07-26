package com.siso.backend.newsletter;

import com.siso.backend.pair.TopicPair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NewsletterSendServiceTest {

    @Mock
    private NewsletterSubscriberRepository newsletterSubscriberRepository;

    @Mock
    private NewsletterDigestService newsletterDigestService;

    @Mock
    private ResendEmailClient resendEmailClient;

    private NewsletterSendService newService() {
        return new NewsletterSendService(
                newsletterSubscriberRepository, newsletterDigestService, resendEmailClient, "http://localhost:3000");
    }

    private NewsletterSubscriber confirmedSubscriber(String email) {
        NewsletterSubscriber subscriber = new NewsletterSubscriber(email, UUID.randomUUID(), OffsetDateTime.now());
        subscriber.confirm(OffsetDateTime.now());
        return subscriber;
    }

    private TopicPair fakePair() {
        return Mockito.mock(TopicPair.class);
    }

    @Test
    void sendWeeklyDigest_clientDisabled_doesNothing() {
        when(resendEmailClient.isEnabled()).thenReturn(false);

        int result = newService().sendWeeklyDigest();

        assertThat(result).isZero();
        verify(newsletterDigestService, never()).findTopPairsForDigest();
    }

    @Test
    void sendWeeklyDigest_noTopPairs_sendsNothing() {
        when(resendEmailClient.isEnabled()).thenReturn(true);
        when(newsletterDigestService.findTopPairsForDigest()).thenReturn(List.of());

        int result = newService().sendWeeklyDigest();

        assertThat(result).isZero();
        verify(newsletterSubscriberRepository, never()).findByStatus(anyString());
    }

    @Test
    void sendWeeklyDigest_sendsToEachConfirmedSubscriberWithOwnUnsubscribeLink() {
        when(resendEmailClient.isEnabled()).thenReturn(true);
        when(newsletterDigestService.findTopPairsForDigest()).thenReturn(List.of(fakePair()));
        NewsletterSubscriber sub1 = confirmedSubscriber("a@example.com");
        NewsletterSubscriber sub2 = confirmedSubscriber("b@example.com");
        when(newsletterSubscriberRepository.findByStatus("confirmed")).thenReturn(List.of(sub1, sub2));
        when(newsletterDigestService.buildDigestHtml(any(), anyString())).thenReturn("<html></html>");

        int result = newService().sendWeeklyDigest();

        assertThat(result).isEqualTo(2);
        verify(newsletterDigestService).buildDigestHtml(any(), contains(sub1.getToken().toString()));
        verify(newsletterDigestService).buildDigestHtml(any(), contains(sub2.getToken().toString()));
        verify(resendEmailClient, times(2)).send(anyString(), anyString(), anyString());
    }

    @Test
    void sendWeeklyDigest_oneSubscriberFails_continuesWithOthers() {
        when(resendEmailClient.isEnabled()).thenReturn(true);
        when(newsletterDigestService.findTopPairsForDigest()).thenReturn(List.of(fakePair()));
        NewsletterSubscriber sub1 = confirmedSubscriber("a@example.com");
        NewsletterSubscriber sub2 = confirmedSubscriber("b@example.com");
        when(newsletterSubscriberRepository.findByStatus("confirmed")).thenReturn(List.of(sub1, sub2));
        when(newsletterDigestService.buildDigestHtml(any(), anyString())).thenReturn("<html></html>");
        doThrow(new NewsletterSendFailed("API 에러"))
                .when(resendEmailClient)
                .send(eq("a@example.com"), anyString(), anyString());

        int result = newService().sendWeeklyDigest();

        assertThat(result).isEqualTo(1);
        verify(resendEmailClient).send(eq("b@example.com"), anyString(), anyString());
    }
}
