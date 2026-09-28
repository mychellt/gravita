package br.gravita.core.ports.outbound.finance;

import java.util.UUID;

/**
 * Tells {@code masterdata} that a customer's financial position changed (a
 * title was settled, renegotiated or became overdue) so that
 * {@code Customer.currentBalance} and {@code Customer.status} are refreshed via
 * M1's {@code SetCustomerCreditStatusPort}. {@code finance} owns the delinquency
 * rule, so the implementation derives the balance and status from the
 * customer's titles.
 */
public interface UpdateCustomerCreditStatusPort {

	void update(UUID customerId);
}
