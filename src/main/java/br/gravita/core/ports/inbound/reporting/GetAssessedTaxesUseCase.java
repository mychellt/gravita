package br.gravita.core.ports.inbound.reporting;

public interface GetAssessedTaxesUseCase {
	AssessedTaxSummary execute(AssessedTaxesQuery query);
}
