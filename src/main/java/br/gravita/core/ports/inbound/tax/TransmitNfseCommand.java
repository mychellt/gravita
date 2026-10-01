package br.gravita.core.ports.inbound.tax;

import br.gravita.core.domain.tax.NfseId;
import java.util.Objects;

public record TransmitNfseCommand(NfseId nfseId) {

	public TransmitNfseCommand {
		Objects.requireNonNull(nfseId, "nfseId");
	}
}
