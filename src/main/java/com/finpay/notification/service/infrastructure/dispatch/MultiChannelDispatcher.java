package com.finpay.notification.service.infrastructure.dispatch;

import com.finpay.notification.service.domain.ChannelDispatcher;
import com.finpay.notification.service.domain.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Multi-channel dispatcher (FP-19/48) with failure isolation: a failure on one
 * channel does not block others (each notification has exactly one channel here,
 * but the dispatcher isolates transport errors and lets the processor retry /
 * dead-letter). A real impl would call SES/Twilio/FCM with per-channel
 * timeout/retry/circuit-breaker (Rule 8).
 */
@Component
public class MultiChannelDispatcher implements ChannelDispatcher {

    private static final Logger log = LoggerFactory.getLogger(MultiChannelDispatcher.class);
    private final AtomicInteger simulatedFailures = new AtomicInteger(0);

    @Override
    public void dispatch(Notification n) throws DispatchException {
        try {
            switch (n.channel()) {
                case EMAIL -> sendEmail(n);
                case SMS -> sendSms(n);
                case PUSH -> sendPush(n);
            }
            log.info("dispatched {} via {}", n.notificationId(), n.channel());
        } catch (RuntimeException ex) {
            throw new DispatchException(ex.getMessage());
        }
    }

    private void sendEmail(Notification n) {
        // transport call would go here; simulated gate for tests
        if (shouldSimulateFailure()) throw new IllegalStateException("email gateway 503");
    }
    private void sendSms(Notification n) {
        if (shouldSimulateFailure()) throw new IllegalStateException("sms provider timeout");
    }
    private void sendPush(Notification n) {
        if (shouldSimulateFailure()) throw new IllegalStateException("push service unreachable");
    }

    /** Test hook: force next dispatch to fail. */
    public void setFailNext(boolean fail) { simulatedFailures.set(fail ? 1 : 0); }
    private boolean shouldSimulateFailure() { return simulatedFailures.getAndDecrement() > 0; }
}
