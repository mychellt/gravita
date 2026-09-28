package br.gravita.core.ports.inbound.finance;

import br.gravita.core.domain.finance.Receivable;
import java.util.List;

/**
 * UC-M8-01: creates one {@code Receivable} per installment of an issued fiscal
 * document's payment terms ({@code INVOICING} origin, {@code OPEN} status).
 * Never called by a user: M7's {@code GenerateAccountsReceivablePort} invokes it
 * after NFe/NFCe issuance and, once M4 lands, so will the NFSe-authorization
 * flow. Idempotent per {@code originDocumentRef}: when the document already has
 * receivables they are returned as-is and nothing is created.
 */
public interface GenerateReceivableFromInvoicingUseCase {

	List<Receivable> execute(GenerateReceivableFromInvoicingCommand command);
}
