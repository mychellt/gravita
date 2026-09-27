package br.gravita.core.domain.tax;

/**
 * NFe lifecycle (doc §3.2 / m2-fiscal-nfe.md domain model). {@code DRAFT} →
 * {@code QUEUED} is exercised by {@code IssueNfeUseCase} (UC-M2-01);
 * {@code QUEUED} → {@code SENT} → {@code AUTHORIZED}/{@code REJECTED} is
 * exercised by {@code TransmitNfeUseCase} (UC-M2-03); the remaining
 * transitions belong to later use cases ({@code CancelNfeUseCase}, ...).
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
