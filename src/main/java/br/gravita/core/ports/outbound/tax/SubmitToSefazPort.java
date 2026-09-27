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

	/**
	 * UC-M2-06: transmits an Inutilização event, formally voiding a range of
	 * document numbers that were allocated but never used.
	 *
	 * @throws SefazUnavailableException when SEFAZ-UF can't be reached.
	 */
	SefazSubmissionResult voidNumberRange(SefazVoidNumberRangeRequest request);

	/**
	 * UC-M2-07: transmits the recipient's manifestation (confirmed / unknown /
	 * operation-not-performed) on an inbound NFe issued against this company by
	 * a third party. Unlike {@link #submit}/{@link #cancel}/
	 * {@link #voidNumberRange}, this call isn't routed through a per-company
	 * mTLS client: the use case works by access key alone (it may not even
	 * have a local company/certificate to resolve - see UC-M2-07's AC3), so a
	 * real production wiring for this method needs its own company/identity
	 * resolution design, tracked as a follow-up.
	 *
	 * @throws SefazUnavailableException when SEFAZ-UF can't be reached.
	 */
	SefazSubmissionResult manifest(SefazManifestationRequest request);
}
