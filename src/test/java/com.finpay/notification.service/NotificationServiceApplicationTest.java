package com.finpay.notification.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Placeholder for the legacy flat package (canonical app in com/finpay/notification/service). */
class NotificationServiceApplicationTest {
    @Test
    void legacyBootstrapLoads() {
        assertThat(NotificationServiceLegacyBootstrap.class).isNotNull();
    }
}
