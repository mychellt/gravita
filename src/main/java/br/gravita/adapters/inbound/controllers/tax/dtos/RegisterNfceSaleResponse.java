package br.gravita.adapters.inbound.controllers.tax.dtos;

import br.gravita.core.domain.tax.NfceSaleId;
import br.gravita.core.domain.tax.NfceSaleStatus;
import br.gravita.core.ports.inbound.tax.NfceIssuanceResult;
import java.util.UUID;

/** {@code issuanceError} is set only when the sale was stored but its NFC-e could not be issued (status stays DRAFT). */
public record RegisterNfceSaleResponse(UUID id, NfceSaleStatus status, String accessKey, String protocol, String issuanceError) {

	public static RegisterNfceSaleResponse from(final NfceSaleId id, final NfceIssuanceResult issuance) {
		return new RegisterNfceSaleResponse(id.value(), issuance.status(), issuance.accessKey(), issuance.protocol(), null);
	}

	public static RegisterNfceSaleResponse notIssued(final NfceSaleId id, final String reason) {
		return new RegisterNfceSaleResponse(id.value(), NfceSaleStatus.DRAFT, null, null, reason == null ? "unknown error" : reason);
	}
}
