package br.gravita.adapters.outbound.integration.tax;

import br.gravita.core.ports.outbound.tax.NotifyPayableGeneratedPort;
import org.springframework.stereotype.Component;

/**
 * Extension point for the finance context (M8), which owns accounts payable.
 * Tax has no dependency on M8, so - mirroring purchasing's own
 * {@code GeneratePayableFromReceiptAdapter} - this is a no-op stub until M8's
 * payable-generation use case (GRA-14) lands and supplies the real adapter.
 */
@Component
class NotifyPayableGeneratedAdapter implements NotifyPayableGeneratedPort {

	@Override
	public void notifyGenerated(NotifyPayableGeneratedCommand command) {
		// No-op until M8 (finance) lands its payable-generation use case (GRA-14).
	}
}
