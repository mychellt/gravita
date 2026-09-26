package br.gravita.core.domain.tax;

/**
 * Full lifecycle from the M3 module spec's domain model. Only {@code DRAFT}
 * is producible by {@link br.gravita.core.ports.inbound.tax.RegisterNfceSaleUseCase}
 * (UC-M3-03); the rest are set by later use cases (issuance, cancellation,
 * contingency sync) once those land.
 */
public enum NfceSaleStatus {
	DRAFT,
	AUTHORIZED,
	PENDING_SYNC,
	CANCELLED,
	VOIDED
}
