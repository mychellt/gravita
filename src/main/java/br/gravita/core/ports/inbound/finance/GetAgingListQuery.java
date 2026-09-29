package br.gravita.core.ports.inbound.finance;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Every component is optional. {@code customerId} restricts the report to one
 * customer's titles; {@code costCenterId} is accepted but, as receivables are
 * not charged to a cost center, a report restricted to one is empty;
 * {@code asOfDate} is the date the days overdue are counted to (default today).
 */
public record GetAgingListQuery(UUID customerId, UUID costCenterId, LocalDate asOfDate) {
}
