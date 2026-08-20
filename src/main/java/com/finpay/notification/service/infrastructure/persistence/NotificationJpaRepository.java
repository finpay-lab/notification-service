package com.finpay.notification.service.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationJpaRepository extends JpaRepository<NotificationEntity, String> {
    Optional<NotificationEntity> findByEventId(String eventId);
}
