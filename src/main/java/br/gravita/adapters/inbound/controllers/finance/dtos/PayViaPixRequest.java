package br.gravita.adapters.inbound.controllers.finance.dtos;

import br.gravita.core.ports.inbound.finance.PayViaPixCommand;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record PayViaPixRequest(@NotBlank String pixKey) {

	public PayViaPixCommand toCommand(UUID payableId) {
		return new PayViaPixCommand(payableId, pixKey);
	}
}
