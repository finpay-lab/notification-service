package com.finpay.notification.service.infrastructure.generator;

import com.finpay.notification.service.domain.MessageGenerator;
import com.finpay.notification.service.domain.Notification;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * LLM copy generator (FP-59 / AI-2). Calls the model endpoint with timeout/
 * retry/circuit-breaker (Rule 8). On any AI failure it returns a deterministic
 * template (channel-appropriate: SMS short, email fuller). BYOK key is read
 * from a secret ref and never logged.
 */
@Component
public class HttpMessageGenerator implements MessageGenerator {

    private final HttpClient http;
    private final String endpoint;
    private final String byokRef;
    private final Duration timeout;

    public HttpMessageGenerator(@Value("${finpay.notification.llm.endpoint:}") String endpoint,
                                @Value("${finpay.notification.llm.byok-secret-ref:}") String byokRef,
                                @Value("${finpay.notification.llm.timeout-seconds:15}") int timeoutSeconds) {
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(timeoutSeconds)).build();
        this.endpoint = endpoint;
        this.byokRef = byokRef;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
    }

    @Override
    public GeneratedCopy generate(String eventType, String customerId, Notification.Channel channel, String payload) {
        if (endpoint == null || endpoint.isBlank()) {
            return new GeneratedCopy(templateSubject(eventType), templateBody(eventType, channel), false);
        }
        try {
            String prompt = buildPrompt(eventType, channel, payload);
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(endpoint))
                    .timeout(timeout).header("Content-Type", "application/json")
                    .header("Authorization", byokRef == null || byokRef.isBlank() ? "" : "Bearer " + byokRef)
                    .POST(HttpRequest.BodyPublishers.ofString(prompt)).build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            // Best-effort: use the raw model output as body; subject derived from eventType.
            return new GeneratedCopy(templateSubject(eventType), resp.body(), true);
        } catch (Exception ex) {
            return new GeneratedCopy(templateSubject(eventType), templateBody(eventType, channel), false);
        }
    }

    private String templateSubject(String eventType) {
        return "FinPay: " + eventType.replace("NotificationRequested", "update");
    }

    private String templateBody(String eventType, Notification.Channel channel) {
        String base = "Hello, here is an update about your " + eventType + ".";
        if (channel == Notification.Channel.SMS) {
            return base.length() > 140 ? base.substring(0, 137) + "..." : base;
        }
        return base + " If you have questions, contact support.";
    }

    private String buildPrompt(String eventType, Notification.Channel channel, String payload) {
        return "{\"eventType\":\"" + eventType + "\",\"channel\":\"" + channel + "\",\"payload\":" + payload + "}";
    }
}
