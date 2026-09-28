package br.gravita.core.ports.outbound.finance;

import br.gravita.core.domain.CustomerStatus;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Tells {@code masterdata} (M1's {@code SetCustomerCreditStatusUseCase}) the
 * customer's latest financial position whenever it changes - a title is
 * settled or renegotiated, becomes overdue. {@code finance} computes the
 * position and {@code masterdata} stores it as given.
 */
public interface UpdateCustomerCreditStatusPort {

	void update(UUID customerId, BigDecimal currentBalance, CustomerStatus status);
}
