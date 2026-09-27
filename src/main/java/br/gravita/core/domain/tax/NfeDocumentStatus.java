package br.gravita.core.domain.tax;

/**
 * NFe lifecycle (doc §3.2 / m2-fiscal-nfe.md domain model). Only
 * {@code DRAFT} → {@code QUEUED} is exercised by {@code IssueNfeUseCase}
 * (UC-M2-01); the remaining transitions belong to later use cases
 * ({@code TransmitNfeUseCase}, {@code CancelNfeUseCase}, ...).
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
