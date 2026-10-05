package br.gravita.core.ports.outbound.tax;

/**
 * {@code rejectionReason} is present only for the "SEFAZ actively rejected
 * this document" outcome (UC-M2-03) - distinct from
 * {@link br.gravita.core.domain.tax.SefazUnavailableException}, which means
 * SEFAZ couldn't be reached at all. Exactly one of {@code protocol}/
 * {@code rejectionReason} is non-null.
 */
public record SefazSubmissionResult(String protocol, String rejectionReason) {

	public SefazSubmissionResult {
		if (protocol == null && rejectionReason == null) {
			throw new IllegalArgumentException("either protocol or rejectionReason must be present");
		}
	}

	/**
	 * The success-only shape every existing caller (NFC-e's issuance/
	 * cancellation/void/manifestation flows) already uses.
	 */
	public SefazSubmissionResult(final String protocol) {
		this(protocol, null);
	}
}
