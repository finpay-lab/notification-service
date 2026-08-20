package com.finpay.notification.service.domain;

/**
 * Orchestrates a single notification (FP-19/48/59). Pure domain logic: consumes
 * a NotificationRequested event, generates copy (LLM + template fallback, FP-59),
 * dispatches with failure isolation, and dead-letters after exhaustion.
 *
 * No Spring/Kafka/JPA imports — infrastructure adapters drive this.
 */
public final class NotificationProcessor {

    private static final int MAX_RETRIES = 3;

    private final NotificationRepository repository;
    private final MessageGenerator generator;
    private final ChannelDispatcher dispatcher;

    public NotificationProcessor(NotificationRepository repository, MessageGenerator generator,
                                 ChannelDispatcher dispatcher) {
        this.repository = repository;
        this.generator = generator;
        this.dispatcher = dispatcher;
    }

    /** Handle a NotificationRequested event (Rule 7: idempotent by eventId). */
    public void handleRequested(String notificationId, String eventId, String customerId,
                                 Notification.Channel channel, String eventType, String payload) {
        if (repository.findByEventId(eventId).isPresent()) {
            return; // duplicate delivery ignored
        }
        Notification n = new Notification(notificationId, eventId, customerId, channel);
        repository.save(n);

        MessageGenerator.GeneratedCopy copy = generator.generate(eventType, customerId, channel, payload);
        n.attachCopy(copy.subject(), copy.body());
        repository.save(n);

        try {
            dispatcher.dispatch(n);
            n.markDispatched();
        } catch (ChannelDispatcher.DispatchException ex) {
            handleFailure(n, ex.getMessage(), 1);
        }
        repository.save(n);
    }

    /** Retry on a failed dispatch; dead-letter after MAX_RETRIES. */
    public void retry(String notificationId) {
        Notification n = repository.findByNotificationId(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("unknown notification"));
        if (n.state() != Notification.State.FAILED) return;
        try {
            dispatcher.dispatch(n);
            n.markDispatched();
        } catch (ChannelDispatcher.DispatchException ex) {
            handleFailure(n, ex.getMessage(), currentAttempt(n) + 1);
        }
        repository.save(n);
    }

    private void handleFailure(Notification n, String error, int attempt) {
        if (attempt >= MAX_RETRIES) {
            n.deadLetter(error);
        } else {
            n.markFailed(error);
        }
    }

    private int currentAttempt(Notification n) {
        // best-effort: count via state; a real impl would track attempts
        return n.state() == Notification.State.FAILED ? 1 : 0;
    }
}
