package br.gravita.core.ports.inbound.tax;

import br.gravita.core.domain.tax.InboundManifestation;

public interface ManifestInboundNfeUseCase {

	InboundManifestation execute(ManifestInboundNfeCommand command);
}
