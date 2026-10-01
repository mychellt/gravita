package br.gravita.core.ports.inbound.tax;

import br.gravita.core.domain.tax.RpsId;

public interface IssueRpsUseCase {

	RpsId execute(IssueRpsCommand command);
}
