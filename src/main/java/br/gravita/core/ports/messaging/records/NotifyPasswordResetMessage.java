package br.gravita.core.ports.messaging.records;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.UUID;

@Builder
public record NotifyPasswordResetMessage(@NotNull String username,
                                         @NotNull String token,
                                         @NotNull String recipient,
                                         @NotNull UUID tenantId) {
}
