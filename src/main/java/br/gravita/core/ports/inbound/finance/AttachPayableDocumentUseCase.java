package br.gravita.core.ports.inbound.finance;

import br.gravita.core.domain.finance.Payable;

/**
 * UC-M8-15: uploads a supporting document (boleto, NF, receipt) and links it
 * to a payable. A payable can carry any number of attachments; each upload
 * appends to the existing ones.
 */
public interface AttachPayableDocumentUseCase {

	Payable execute(AttachPayableDocumentCommand command);
}
