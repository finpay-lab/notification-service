package com.finpay.notification.service.domain;

/** Dispatches a notification to its channel (FP-19/48). Failure isolation per-channel. */
public interface ChannelDispatcher {
    /** Returns normally on success; throws on delivery failure (caller handles retry/dead-letter). */
    void dispatch(Notification notification) throws DispatchException;

    class DispatchException extends Exception {
        public DispatchException(String msg) { super(msg); }
    }
}
