package br.gravita.core.ports.inbound.tax;

import br.gravita.core.domain.tax.NfeDocument;

/**
 * UC-M2-01: creates and queues a new outbound (B2B) NFe, triggered by a
 * user's manual entry or automatically by M7's {@code InvoiceSalesOrderUseCase}
 * when a sales order is invoiced. Builds an {@link NfeDocument} in
 * {@code DRAFT}, resolves item-level taxes via {@code CalculateTaxUseCase},
 * allocates the next document number, and enqueues it for transmission
 * (UC-M2-03), leaving it {@code QUEUED}.
 */
public interface IssueNfeUseCase {
	NfeDocument execute(IssueNfeCommand command);
}
