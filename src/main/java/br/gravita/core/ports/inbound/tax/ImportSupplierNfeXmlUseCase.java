package br.gravita.core.ports.inbound.tax;

import br.gravita.core.domain.tax.InboundNfe;

/**
 * UC-M2-08: parses a supplier-issued NFe XML into a new {@code InboundNfe},
 * auto-filling supplier, items, quantities, values and taxes. Triggered by a
 * direct upload or by M6's {@code ImportSupplierNfeAtReceivingUseCase}
 * during purchase receiving.
 */
public interface ImportSupplierNfeXmlUseCase {
	InboundNfe execute(ImportSupplierNfeXmlCommand command);
}
