package br.gravita.core.ports.inbound.tax;

/**
 * Shared tax engine port (UC-M2-02). Invoked synchronously by every
 * fiscal-document issuance use case (NFe, NFCe, NFSe) and by sales/purchasing
 * for previews and inbound tax-credit computation — one implementation, no
 * per-module duplication (docs/specs/m2-fiscal-nfe/uc-02-calculate-tax.md).
 */
public interface CalculateTaxUseCase {

	TaxCalculationResult execute(CalculateTaxCommand command);
}
