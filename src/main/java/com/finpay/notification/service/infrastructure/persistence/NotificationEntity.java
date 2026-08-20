package com.finpay.notification.service.infrastructure.persistence;

import com.finpay.notification.service.domain.Notification;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "notifications")
public class NotificationEntity {

    @Id
    private String notificationId;
    private String eventId;
    private String customerId;
    private String channel;
    private String subject;
    private String body;
    private String state;
    private String lastError;
    private Instant createdAt;
    private Instant updatedAt;

    public NotificationEntity() {}

    public static NotificationEntity from(Notification n) {
        NotificationEntity e = new NotificationEntity();
        e.notificationId = n.notificationId();
        e.eventId = n.eventId();
        e.customerId = n.customerId();
        e.channel = n.channel().name();
        e.subject = n.subject();
        e.body = n.body();
        e.state = n.state().name();
        e.lastError = n.lastError();
        e.createdAt = n.createdAt();
        e.updatedAt = Instant.now();
        return e;
    }

    public Notification toDomain() {
        Notification n = new Notification(notificationId, eventId, customerId,
                Notification.Channel.valueOf(channel));
        if (n.state() == Notification.State.RECEIVED && subject != null) {
            n.attachCopy(subject, body);
        }
        if (state.equals("DISPATCHED")) n.markDispatched();
        else if (state.equals("FAILED")) n.markFailed(lastError == null ? "retry" : lastError);
        else if (state.equals("DEAD_LETTER")) { n.markFailed(lastError); n.deadLetter(lastError); }
        return n;
    }

    public String getNotificationId() { return notificationId; }
    public void setNotificationId(String v) { this.notificationId = v; }
    public String getEventId() { return eventId; }
    public void setEventId(String v) { this.eventId = v; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String v) { this.customerId = v; }
    public String getChannel() { return channel; }
    public void setChannel(String v) { this.channel = v; }
    public String getSubject() { return subject; }
    public void setSubject(String v) { this.subject = v; }
    public String getBody() { return body; }
    public void setBody(String v) { this.body = v; }
    public String getState() { return state; }
    public void setState(String v) { this.state = v; }
    public String getLastError() { return lastError; }
    public void setLastError(String v) { this.lastError = v; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant v) { this.createdAt = v; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant v) { this.updatedAt = v; }
}
