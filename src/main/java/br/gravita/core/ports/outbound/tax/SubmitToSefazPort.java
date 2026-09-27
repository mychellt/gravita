package br.gravita.core.ports.outbound.tax;

import br.gravita.core.domain.tax.SefazUnavailableException;

public interface SubmitToSefazPort {

	SefazSubmissionResult submit(SefazSubmissionRequest request);

	SefazSubmissionResult cancel(SefazCancellationRequest request);

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
