package br.gravita.core.ports.outbound.persistence.finance;

import br.gravita.core.domain.finance.Receivable;

public interface ReceivableRepositoryPort {
	Receivable save(Receivable receivable);
}
