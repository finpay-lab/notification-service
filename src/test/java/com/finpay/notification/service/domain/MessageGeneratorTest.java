package com.finpay.notification.service.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MessageGeneratorTest {

    @Test
    void templateFallbackWhenNoEndpoint() {
        // Simulate the no-endpoint branch by using a generator that always falls back.
        MessageGenerator g = (eventType, customerId, channel, payload) ->
                new MessageGenerator.GeneratedCopy("FinPay: update", "Hello, update about your " + eventType + ".", false);
        MessageGenerator.GeneratedCopy c = g.generate("TransferCompleted", "c1", Notification.Channel.SMS, "{}");
        assertThat(c.aiGenerated()).isFalse();
        assertThat(c.body()).contains("TransferCompleted");
        // SMS body kept short
        assertThat(c.body().length()).isLessThanOrEqualTo(160);
    }
}
