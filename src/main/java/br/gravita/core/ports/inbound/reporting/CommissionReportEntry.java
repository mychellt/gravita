package br.gravita.core.ports.inbound.reporting;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * One commission line of the period: what a salesperson earned on a product of an invoiced order. {@code rate} is
 * the one applied when the commission was calculated and {@code amount} the commission itself, so a payroll input
 * is the sum of {@code amount} per salesperson. It is a read model projected from {@code sales.Commission}.
 */
public record CommissionReportEntry(UUID salespersonId, UUID productId, UUID orderId, BigDecimal rate,
		BigDecimal amount) {
}
