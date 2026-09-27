package br.gravita.core.domain.tax;

/**
 * NFe (modelo 55) lifecycle (module spec, doc §3.2). UC-M2-01 only produces
 * {@code DRAFT} then {@code QUEUED}; the remaining states are reached by
 * later use cases (UC-M2-03 transmission, UC-M2-04 cancellation, ...).
 */
public enum NfeDocumentStatus {
	DRAFT,
	QUEUED,
	SENT,
	AUTHORIZED,
	REJECTED,
	CANCELLED,
	VOIDED
}
