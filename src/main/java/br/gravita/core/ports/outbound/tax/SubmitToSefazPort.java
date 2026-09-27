package br.gravita.core.ports.outbound.tax;

import br.gravita.core.domain.tax.SefazUnavailableException;

/**
 * Real-time SEFAZ-UF authorization (module spec: "shared with M2", but M2's
 * own NFe issuance has no adapter yet - see GRA-92's build-order note). The
 * NFC-e case builds its own certificate-based client here; M2 can adopt it
 * once its issuance ticket exists.
 */
public interface SubmitToSefazPort {

	/**
	 * @throws SefazUnavailableException when SEFAZ-UF can't be reached in
	 * time (AC2) - never a checked exception, since every caller's only
	 * useful reaction is the same: queue for contingency.
	 */
	SefazSubmissionResult submit(SefazSubmissionRequest request);

	/**
	 * UC-M3-07 (AC4): transmits a cancellation event for an already-authorized
	 * document.
	 *
	 * @throws SefazUnavailableException when SEFAZ-UF can't be reached.
	 */
	SefazSubmissionResult cancel(SefazCancellationRequest request);
}
