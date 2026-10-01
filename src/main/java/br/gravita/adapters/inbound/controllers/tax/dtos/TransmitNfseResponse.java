package br.gravita.adapters.inbound.controllers.tax.dtos;

import br.gravita.core.ports.inbound.tax.NfseTransmissionResult;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

/**
 * Flat view of {@link NfseTransmissionResult}: {@code outcome} says which of the three decided results it is, and only
 * that result's fields are present. All three answer {@code 200} - a rejection or a manual-upload hand-off is a normal
 * result of the call, not a failure of it.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TransmitNfseResponse(Outcome outcome, String protocol, Instant authorizedAt, String rejectionReason,
		String xml, String instructions) {

	public enum Outcome {
		AUTHORIZED,
		REJECTED,
		MANUAL_UPLOAD_REQUIRED
	}

	public static TransmitNfseResponse from(NfseTransmissionResult result) {
		return switch (result) {
			case NfseTransmissionResult.Authorized authorized -> new TransmitNfseResponse(Outcome.AUTHORIZED,
					authorized.protocol(), authorized.authorizedAt(), null, null, null);
			case NfseTransmissionResult.Rejected rejected -> new TransmitNfseResponse(Outcome.REJECTED, null, null,
					rejected.reason(), null, null);
			case NfseTransmissionResult.GuidedManualUpload upload -> new TransmitNfseResponse(
					Outcome.MANUAL_UPLOAD_REQUIRED, null, null, null, upload.xml(), upload.instructions());
		};
	}
}
