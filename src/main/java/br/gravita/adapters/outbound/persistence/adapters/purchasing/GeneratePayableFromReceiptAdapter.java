package br.gravita.adapters.outbound.persistence.adapters.purchasing;

import br.gravita.core.ports.outbound.persistence.purchasing.GeneratePayableFromReceiptPort;
import org.springframework.stereotype.Component;

/**
 * Finance (M8) does not exist yet, so there is no accounts-payable ledger to
 * write to. This stub keeps {@code ConfirmPurchaseReceiptService} runnable
 * end-to-end until M8 ships its own {@link GeneratePayableFromReceiptPort}
 * adapter backed by real payable tables.
 */
@Component
class GeneratePayableFromReceiptAdapter implements GeneratePayableFromReceiptPort {

	@Override
	public void generatePayables(GeneratePayableFromReceiptCommand command) {
		// No-op until M8 (finance) lands its payable-generation use case (GRA-14).
	}
}
