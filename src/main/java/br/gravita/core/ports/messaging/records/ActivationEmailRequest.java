package br.gravita.core.ports.messaging.records;

import lombok.Builder;
import lombok.NonNull;

import java.time.Instant;

@Builder
public record ActivationEmailRequest(
        @NonNull String to,
        @NonNull String recipientName,
        @NonNull String activationToken,
        @NonNull Instant expiresAt) {
}
