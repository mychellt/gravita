package br.gravita.core.ports.inbound.system;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** A subscription payment already received. */
public record PlatformPaymentView(UUID id, BigDecimal amount, LocalDate paymentDate) {
}
