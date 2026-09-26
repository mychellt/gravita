package br.gravita.adapters.inbound.controllers.tax.dtos;

import br.gravita.core.domain.tax.NfceSaleId;
import java.util.UUID;

public record RegisterNfceSaleResponse(UUID id) {

	public static RegisterNfceSaleResponse from(NfceSaleId id) {
		return new RegisterNfceSaleResponse(id.value());
	}
}
