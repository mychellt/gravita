package br.gravita.core.ports.inbound.tax;

/**
 * UC-M3-04: takes the DRAFT sale from UC-03, calculates taxes via the shared
 * {@link CalculateTaxUseCase} (M2), allocates a document number, and attempts
 * real-time SEFAZ authorization - falling back to contingency queuing
 * (AC2) instead of blocking the cashier when SEFAZ-UF is unavailable.
 */
public interface IssueNfceUseCase {

	NfceIssuanceResult execute(IssueNfceCommand command);
}
