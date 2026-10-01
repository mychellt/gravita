package br.gravita.core.domain.tax;

/**
 * Lifecycle of an {@link NfseDocument}. {@code RPS} is the pre-conversion state of an internally issued RPS; the
 * remaining values are the doc §5.2 lifecycle (Draft → Sent → Authorized → Cancelled).
 */
public enum NfseStatus {
	RPS,
	DRAFT,
	SENT,
	AUTHORIZED,
	CANCELLED
}
