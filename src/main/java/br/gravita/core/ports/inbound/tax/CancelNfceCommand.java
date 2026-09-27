package br.gravita.core.ports.inbound.tax;

import java.util.Objects;
import java.util.UUID;

public record CancelNfceCommand(UUID nfceSaleId, String supervisorCredential, String reason) {

	public CancelNfceCommand {
		Objects.requireNonNull(nfceSaleId, "nfceSaleId is required");
		Objects.requireNonNull(supervisorCredential, "supervisorCredential is required");
	}
}
