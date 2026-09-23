package br.gravita.adapters.outbound.persistence.adapters.purchasing;

import br.gravita.core.ports.outbound.persistence.purchasing.ReversePayableFromReturnPort;
import org.springframework.stereotype.Component;

/**
 * Finance (M8) does not exist yet, so there is no accounts-payable ledger to
 * write to. This stub keeps {@code ReturnToSupplierService} runnable
 * end-to-end until M8 ships its own {@link ReversePayableFromReturnPort}
 * adapter backed by real payable tables.
 */
@Component
class ReversePayableFromReturnAdapter implements ReversePayableFromReturnPort {

	@Override
	public void reversePayable(ReversePayableFromReturnCommand command) {
		// No-op until M8 (finance) lands its payable-generation use case (GRA-14).
	}
}
