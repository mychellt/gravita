package br.gravita.core.ports.outbound.persistence.finance;

import br.gravita.core.domain.finance.Boleto;
import br.gravita.core.domain.finance.ReceivableId;
import java.util.List;

public interface BoletoRepositoryPort {
	Boleto save(Boleto boleto);

	List<Boleto> findByReceivableId(ReceivableId receivableId);
}
