package com.finpay.notification.service.domain;

/** Generates channel-appropriate copy (FP-59 / AI-2). Domain port; LLM impl in infrastructure/. */
public interface MessageGenerator {
    /** Returns copy; on AI failure, falls back to a static template (never throws). */
    GeneratedCopy generate(String eventType, String customerId, Notification.Channel channel, String payload);

    record GeneratedCopy(String subject, String body, boolean aiGenerated) {}
}
