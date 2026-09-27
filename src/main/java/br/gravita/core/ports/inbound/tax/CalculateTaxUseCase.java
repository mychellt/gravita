package br.gravita.core.ports.inbound.tax;

public interface CalculateTaxUseCase {

	TaxCalculationResult execute(CalculateTaxCommand command);
}
