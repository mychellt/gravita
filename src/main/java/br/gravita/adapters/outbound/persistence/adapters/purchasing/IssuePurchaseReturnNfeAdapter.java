package br.gravita.adapters.outbound.persistence.adapters.purchasing;

import br.gravita.core.ports.outbound.persistence.purchasing.IssuePurchaseReturnNfePort;
import org.springframework.stereotype.Component;

/**
 * M2's {@code IssueNfeUseCase} (`tax` context, GRA-8) does not exist yet, so
 * there is no NF-e to actually transmit. This stub keeps
 * {@code ReturnToSupplierService} runnable end-to-end - the return is still
 * recorded with a {@code null} {@code returnNfeRef} - until M2 ships a real
 * adapter that delegates to {@code IssueNfeUseCase} and returns its access
 * key.
 */
@Component
class IssuePurchaseReturnNfeAdapter implements IssuePurchaseReturnNfePort {

	@Override
	public String issueReturnNfe(IssuePurchaseReturnNfeCommand command) {
		// No-op until M2 (tax) lands IssueNfeUseCase (GRA-8).
		return null;
	}
}
