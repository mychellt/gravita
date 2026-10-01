package br.gravita.core.ports.inbound.tax;

import br.gravita.core.domain.tax.NfseId;
import java.util.Objects;

public record CancelNfseCommand(NfseId nfseId, String justification) {

	public CancelNfseCommand {
		Objects.requireNonNull(nfseId, "nfseId");
	}
}
