package br.gravita.core.ports.inbound.purchasing;

import br.gravita.core.domain.purchasing.ConferenceResult;

/**
 * UC-M6-07: reconciles the supplier's NF-e XML against a
 * {@link br.gravita.core.domain.purchasing.PurchaseOrder} and its
 * already-recorded {@link br.gravita.core.domain.purchasing.PurchaseReceipt}
 * (UC-M6-06), delegating the actual XML parsing to M2's
 * {@code ImportSupplierNfeXmlUseCase}.
 */
public interface ImportSupplierNfeAtReceivingUseCase {
	ConferenceResult execute(ImportSupplierNfeAtReceivingCommand command);
}
