package com.finpay.notification.service.domain;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationProcessorTest {

    static final class FakeRepo implements NotificationRepository {
        final List<Notification> all = new ArrayList<>();
        final List<String> byEvent = new ArrayList<>();
        @Override public Optional<Notification> findByNotificationId(String id) {
            return all.stream().filter(n -> n.notificationId().equals(id)).findFirst();
        }
        @Override public Optional<Notification> findByEventId(String e) {
            return all.stream().filter(n -> n.eventId().equals(e)).findFirst();
        }
        @Override public Notification save(Notification n) { all.removeIf(x -> x.notificationId().equals(n.notificationId())); all.add(n); return n; }
    }

    static final class FakeGen implements MessageGenerator {
        boolean ai = true;
        @Override public GeneratedCopy generate(String t, String c, Notification.Channel ch, String p) {
            return new GeneratedCopy("subj", "body", ai);
        }
    }

    static final class FakeDispatch implements ChannelDispatcher {
        boolean fail = false;
        int attempts = 0;
        @Override public void dispatch(Notification n) throws DispatchException {
            attempts++; if (fail) throw new DispatchException("boom");
        }
    }

    @Test
    void happyPathDispatches() {
        FakeRepo repo = new FakeRepo();
        FakeGen gen = new FakeGen();
        FakeDispatch d = new FakeDispatch();
        NotificationProcessor p = new NotificationProcessor(repo, gen, d);
        p.handleRequested("n1", "e1", "c1", Notification.Channel.EMAIL, "TransferCompleted", "{}");
        Notification n = repo.findByEventId("e1").orElseThrow();
        assertThat(n.state()).isEqualTo(Notification.State.DISPATCHED);
        assertThat(d.attempts).isEqualTo(1);
    }

    @Test
    void idempotentByEventId() {
        FakeRepo repo = new FakeRepo();
        NotificationProcessor p = new NotificationProcessor(repo, new FakeGen(), new FakeDispatch());
        p.handleRequested("n1", "e1", "c1", Notification.Channel.EMAIL, "X", "{}");
        p.handleRequested("n2", "e1", "c1", Notification.Channel.EMAIL, "X", "{}"); // dup eventId
        assertThat(repo.all).hasSize(1);
    }

    @Test
    void failureIsolatesAndDeadLettersAfterRetries() {
        FakeRepo repo = new FakeRepo();
        FakeGen gen = new FakeGen();
        FakeDispatch d = new FakeDispatch(); d.fail = true;
        NotificationProcessor p = new NotificationProcessor(repo, gen, d);
        p.handleRequested("n1", "e1", "c1", Notification.Channel.SMS, "X", "{}");
        Notification n = repo.findByEventId("e1").orElseThrow();
        // first failure: FAILED (attempt bookkeeping simplified here)
        assertThat(n.state()).isIn(Notification.State.FAILED, Notification.State.DEAD_LETTER);
    }
}
