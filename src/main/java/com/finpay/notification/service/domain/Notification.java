package com.finpay.notification.service.domain;

import java.time.Instant;
import java.util.List;

/**
 * Notification aggregate (FP-19/48). Consumes NotificationRequested, generates
 * copy (FP-59 LLM, with template fallback), dispatches via channel, with
 * failure isolation + dead-letter. Idempotent by notificationId (Rule 7).
 */
public class Notification {

    public enum Channel { EMAIL, SMS, PUSH }
    public enum State { RECEIVED, GENERATED, DISPATCHED, FAILED, DEAD_LETTER }

    private final String notificationId;
    private final String eventId;
    private final String customerId;
    private final Channel channel;
    private String subject;
    private String body;
    private State state;
    private String lastError;
    private final Instant createdAt;
    private Instant updatedAt;

    public Notification(String notificationId, String eventId, String customerId, Channel channel) {
        this.notificationId = notificationId;
        this.eventId = eventId;
        this.customerId = customerId;
        this.channel = channel;
        this.state = State.RECEIVED;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public String notificationId() { return notificationId; }
    public String eventId() { return eventId; }
    public String customerId() { return customerId; }
    public Channel channel() { return channel; }
    public String subject() { return subject; }
    public String body() { return body; }
    public State state() { return state; }
    public String lastError() { return lastError; }
    public Instant createdAt() { return createdAt; }

    /** FP-59: attach generated/fallback copy (RECEIVED -> GENERATED). */
    public void attachCopy(String subject, String body) {
        if (state != State.RECEIVED) throw new IllegalStateTransition(state, "GENERATED");
        this.subject = subject;
        this.body = body;
        this.state = State.GENERATED;
        this.updatedAt = Instant.now();
    }

    /** Rule 9: GENERATED -> DISPATCHED. */
    public void markDispatched() {
        if (state != State.GENERATED) throw new IllegalStateTransition(state, "DISPATCHED");
        this.state = State.DISPATCHED;
        this.updatedAt = Instant.now();
    }

    /** Failure isolation: GENERATED -> FAILED (retryable). */
    public void markFailed(String error) {
        if (state != State.GENERATED) throw new IllegalStateTransition(state, "FAILED");
        this.lastError = error;
        this.state = State.FAILED;
        this.updatedAt = Instant.now();
    }

    /** Dead-letter after exhausted retries (Rule 9: FAILED -> DEAD_LETTER). */
    public void deadLetter(String error) {
        if (state != State.FAILED) throw new IllegalStateTransition(state, "DEAD_LETTER");
        this.lastError = error;
        this.state = State.DEAD_LETTER;
        this.updatedAt = Instant.now();
    }

    public static final class IllegalStateTransition extends RuntimeException {
        IllegalStateTransition(State from, String to) { super("cannot transition " + from + " -> " + to); }
    }
}
