package br.gravita.adapters.outbound.persistence.adapters.sales;

import br.gravita.core.ports.outbound.sales.AdjustReceivableForReturnPort;
import org.springframework.stereotype.Component;

/**
 * {@code finance} (M8) is not implemented yet - mirrors
 * {@code GenerateAccountsReceivableAdapter}: a no-op placeholder until
 * {@code AdjustReceivableForReturnUseCase} exists to wire into.
 */
@Component
class AdjustReceivableForReturnAdapter implements AdjustReceivableForReturnPort {

	@Override
	public void adjust(AdjustReceivableForReturnCommand command) {
	}
}
