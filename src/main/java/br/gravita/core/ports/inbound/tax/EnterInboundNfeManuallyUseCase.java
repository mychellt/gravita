package br.gravita.core.ports.inbound.tax;

import br.gravita.core.domain.tax.InboundNfe;

/**
 * UC-M2-09: enters a purchase NFe from a supplier who didn't provide an XML
 * file - either by typing the access key alone (fetched from SEFAZ when
 * resolvable) or by entering the data fully by hand via {@code manualData}.
 * Produces the same {@code InboundNfe} shape/state as
 * {@link ImportSupplierNfeXmlUseCase}'s XML import, so UC-M2-10's conference
 * doesn't need to branch by entry method.
 */
public interface EnterInboundNfeManuallyUseCase {
	InboundNfe execute(EnterInboundNfeManuallyCommand command);
}
