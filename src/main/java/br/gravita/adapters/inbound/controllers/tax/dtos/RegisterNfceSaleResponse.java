package br.gravita.adapters.inbound.controllers.tax.dtos;

import br.gravita.core.domain.tax.NfceSaleId;
import br.gravita.core.domain.tax.NfceSaleStatus;
import br.gravita.core.ports.inbound.tax.NfceIssuanceResult;
import java.util.UUID;

public record RegisterNfceSaleResponse(UUID id, NfceSaleStatus status, String accessKey, String protocol) {

	public static RegisterNfceSaleResponse from(final NfceSaleId id, final NfceIssuanceResult issuance) {
		return new RegisterNfceSaleResponse(id.value(), issuance.status(), issuance.accessKey(), issuance.protocol());
	}
}
