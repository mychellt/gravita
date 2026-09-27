package br.gravita.core.ports.inbound.tax;

import br.gravita.core.domain.tax.InboundNfe;

public interface ImportSupplierNfeXmlUseCase {
	InboundNfe execute(ImportSupplierNfeXmlCommand command);
}
