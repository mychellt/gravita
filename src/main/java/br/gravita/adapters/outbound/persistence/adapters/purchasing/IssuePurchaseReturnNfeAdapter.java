package br.gravita.adapters.outbound.persistence.adapters.purchasing;

import br.gravita.core.ports.outbound.persistence.purchasing.IssuePurchaseReturnNfePort;
import org.springframework.stereotype.Component;

@Component
class IssuePurchaseReturnNfeAdapter implements IssuePurchaseReturnNfePort {

	@Override
	public String issueReturnNfe(final IssuePurchaseReturnNfeCommand command) {
		return null;
	}
}
