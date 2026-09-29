package br.gravita.core.ports.inbound.finance;

import br.gravita.core.domain.finance.Payable;

/**
 * UC-M8-15: uploads a supporting document (boleto, NF, receipt) and links it to
 * a payable. The file is stored and its reference appended to the payable's
 * attachments; a payable can carry any number of them.
 */
public interface AttachPayableDocumentUseCase {

	Payable execute(AttachPayableDocumentCommand command);
}
