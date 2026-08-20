package com.finpay.notification.service.infrastructure.persistence;

import com.finpay.notification.service.domain.Notification;
import com.finpay.notification.service.domain.NotificationRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@Transactional
public class NotificationRepositoryImpl implements NotificationRepository {

    private final NotificationJpaRepository jpa;

    public NotificationRepositoryImpl(NotificationJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override public Optional<Notification> findByNotificationId(String id) {
        return jpa.findById(id).map(NotificationEntity::toDomain);
    }
    @Override public Optional<Notification> findByEventId(String eventId) {
        return jpa.findByEventId(eventId).map(NotificationEntity::toDomain);
    }
    @Override public Notification save(Notification n) {
        return jpa.save(NotificationEntity.from(n)).toDomain();
    }
}
