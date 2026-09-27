package br.gravita.adapters.inbound.controllers.tax.dtos;

import br.gravita.core.ports.inbound.tax.CancelNfceCommand;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record CancelNfceRequest(@NotBlank String supervisorCredential, String reason) {

	public CancelNfceCommand toCommand(UUID nfceSaleId) {
		return new CancelNfceCommand(nfceSaleId, supervisorCredential, reason);
	}
}
