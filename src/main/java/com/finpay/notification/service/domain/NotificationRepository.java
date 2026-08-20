package com.finpay.notification.service.domain;

import java.util.Optional;

/** Domain port for notification persistence (Rule 4: no JPA imports). */
public interface NotificationRepository {
    Optional<Notification> findByNotificationId(String id);
    Optional<Notification> findByEventId(String eventId);
    Notification save(Notification n);
}
