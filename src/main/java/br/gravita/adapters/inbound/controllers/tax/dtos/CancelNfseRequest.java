package br.gravita.adapters.inbound.controllers.tax.dtos;

import br.gravita.core.domain.tax.NfseId;
import br.gravita.core.ports.inbound.tax.CancelNfseCommand;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record CancelNfseRequest(@NotBlank String justification) {

	public CancelNfseCommand toCommand(final UUID nfseId) {
		return new CancelNfseCommand(NfseId.of(nfseId), justification);
	}
}
