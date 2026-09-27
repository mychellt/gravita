package br.gravita.core.ports.inbound.tax;

import br.gravita.core.domain.tax.NfeDocument;

public interface IssueNfeUseCase {

	NfeDocument execute(IssueNfeCommand command);
}
