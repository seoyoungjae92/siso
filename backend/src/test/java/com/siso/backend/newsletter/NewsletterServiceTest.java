package com.siso.backend.newsletter;

import com.siso.backend.ratelimit.RateLimiter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NewsletterServiceTest {

    @Mock
    private NewsletterSubscriberRepository newsletterSubscriberRepository;

    @Mock
    private ResendEmailClient resendEmailClient;

    @Mock
    private RateLimiter rateLimiter;

    private NewsletterService newService() {
        return new NewsletterService(
                newsletterSubscriberRepository, resendEmailClient, rateLimiter, "http://localhost:3000");
    }

    @Test
    void subscribe_newEmail_savesAsPendingAndSendsConfirmationEmail() {
        when(newsletterSubscriberRepository.findByEmail("a@example.com")).thenReturn(Optional.empty());

        newService().subscribe("a@example.com");

        ArgumentCaptor<NewsletterSubscriber> captor = ArgumentCaptor.forClass(NewsletterSubscriber.class);
        verify(newsletterSubscriberRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("a@example.com");
        assertThat(captor.getValue().getStatus()).isEqualTo("pending");
        verify(resendEmailClient).send(anyString(), anyString(), anyString());
    }

    @Test
    void subscribe_rateLimited_isRejectedBeforeAnyLookup() {
        doThrow(new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS))
                .when(rateLimiter).checkOrThrow("newsletter-subscribe", "a@example.com");

        assertThatThrownBy(() -> newService().subscribe("a@example.com"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);

        verify(newsletterSubscriberRepository, never()).findByEmail(any());
    }

    @Test
    void subscribe_invalidEmail_isRejected() {
        assertThatThrownBy(() -> newService().subscribe("not-an-email"))
                .isInstanceOf(ResponseStatusException.class);

        verify(newsletterSubscriberRepository, never()).save(any());
    }

    @Test
    void subscribe_alreadyConfirmed_isRejected() {
        NewsletterSubscriber subscriber =
                new NewsletterSubscriber("a@example.com", UUID.randomUUID(), OffsetDateTime.now());
        subscriber.confirm(OffsetDateTime.now());
        when(newsletterSubscriberRepository.findByEmail("a@example.com")).thenReturn(Optional.of(subscriber));

        assertThatThrownBy(() -> newService().subscribe("a@example.com"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("이미 구독");
    }

    @Test
    void subscribe_pendingSubscriber_resendsConfirmationWithNewToken() {
        UUID oldToken = UUID.randomUUID();
        NewsletterSubscriber subscriber = new NewsletterSubscriber("a@example.com", oldToken, OffsetDateTime.now());
        when(newsletterSubscriberRepository.findByEmail("a@example.com")).thenReturn(Optional.of(subscriber));

        newService().subscribe("a@example.com");

        assertThat(subscriber.getStatus()).isEqualTo("pending");
        assertThat(subscriber.getToken()).isNotEqualTo(oldToken);
    }

    @Test
    void subscribe_emailSendFails_stillSavesSubscriber() {
        when(newsletterSubscriberRepository.findByEmail("a@example.com")).thenReturn(Optional.empty());
        doThrow(new NewsletterSendFailed("API 에러")).when(resendEmailClient).send(anyString(), anyString(), anyString());

        newService().subscribe("a@example.com");

        verify(newsletterSubscriberRepository).save(any(NewsletterSubscriber.class));
    }

    @Test
    void confirm_pendingSubscriber_becomesConfirmed() {
        UUID token = UUID.randomUUID();
        NewsletterSubscriber subscriber = new NewsletterSubscriber("a@example.com", token, OffsetDateTime.now());
        when(newsletterSubscriberRepository.findByToken(token)).thenReturn(Optional.of(subscriber));

        newService().confirm(token.toString());

        assertThat(subscriber.getStatus()).isEqualTo("confirmed");
        assertThat(subscriber.getConfirmedAt()).isNotNull();
    }

    @Test
    void confirm_unknownToken_throwsNotFound() {
        UUID token = UUID.randomUUID();
        when(newsletterSubscriberRepository.findByToken(token)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> newService().confirm(token.toString())).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void confirm_malformedToken_isRejectedCleanly() {
        assertThatThrownBy(() -> newService().confirm("not-a-uuid"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("유효하지 않은 링크");

        verify(newsletterSubscriberRepository, never()).findByToken(any());
    }

    @Test
    void unsubscribe_confirmedSubscriber_deletesSubscriber() {
        UUID token = UUID.randomUUID();
        NewsletterSubscriber subscriber = new NewsletterSubscriber("a@example.com", token, OffsetDateTime.now());
        subscriber.confirm(OffsetDateTime.now());
        when(newsletterSubscriberRepository.findByToken(token)).thenReturn(Optional.of(subscriber));

        newService().unsubscribe(token.toString());

        verify(newsletterSubscriberRepository).delete(subscriber);
    }

    @Test
    void unsubscribe_malformedToken_isRejectedCleanly() {
        assertThatThrownBy(() -> newService().unsubscribe("not-a-uuid"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("유효하지 않은 링크");
    }
}
