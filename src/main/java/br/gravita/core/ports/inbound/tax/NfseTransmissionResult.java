package br.gravita.core.ports.inbound.tax;

import java.time.Instant;
import java.util.Objects;

/**
 * Outcome of {@link TransmitNfseUseCase}. All three are decided, first-class outcomes - in particular
 * {@link GuidedManualUpload} is how a not-yet-homologated municipality is served, not an error. A municipality that
 * cannot be reached is not an outcome: that surfaces as
 * {@link br.gravita.core.domain.tax.NfseMunicipalityUnavailableException} and leaves the document untouched.
 */
public sealed interface NfseTransmissionResult {

	/** The municipality authorized the NFSe; the document is {@code AUTHORIZED}. */
	record Authorized(String protocol, Instant authorizedAt) implements NfseTransmissionResult {

		public Authorized {
			Objects.requireNonNull(protocol, "protocol");
			Objects.requireNonNull(authorizedAt, "authorizedAt");
		}
	}

	/** The municipality refused the NFSe; the document is back in {@code DRAFT} and can be transmitted again. */
	record Rejected(String reason) implements NfseTransmissionResult {

		public Rejected {
			Objects.requireNonNull(reason, "reason");
		}
	}

	/**
	 * Nothing was transmitted: the municipality has no homologated integration, so the user gets the standard-correct
	 * XML and the steps to upload it on the municipality's own portal. The document stays {@code DRAFT}.
	 */
	record GuidedManualUpload(String xml, String instructions) implements NfseTransmissionResult {

		public GuidedManualUpload {
			Objects.requireNonNull(xml, "xml");
			Objects.requireNonNull(instructions, "instructions");
		}
	}
}
