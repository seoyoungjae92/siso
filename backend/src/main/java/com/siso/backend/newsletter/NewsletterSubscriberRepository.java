package com.siso.backend.newsletter;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NewsletterSubscriberRepository extends JpaRepository<NewsletterSubscriber, Long> {

    Optional<NewsletterSubscriber> findByEmail(String email);

    Optional<NewsletterSubscriber> findByToken(UUID token);

    List<NewsletterSubscriber> findByStatus(String status);

    long countByStatus(String status);
}
