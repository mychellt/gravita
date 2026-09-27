package br.gravita.adapters.outbound.persistence.adapters.sales;

import br.gravita.core.ports.outbound.sales.GenerateAccountsReceivablePort;
import org.springframework.stereotype.Component;

/**
 * {@code finance} (M8) is not implemented yet - mirrors
 * {@code NotifyPayableGeneratedAdapter} in {@code tax}: a no-op placeholder
 * until {@code GenerateReceivableFromInvoicingUseCase} exists to wire into.
 */
@Component
class GenerateAccountsReceivableAdapter implements GenerateAccountsReceivablePort {

	@Override
	public void generate(GenerateAccountsReceivableCommand command) {
	}
}
