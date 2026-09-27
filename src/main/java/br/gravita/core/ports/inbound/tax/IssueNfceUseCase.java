package br.gravita.core.ports.inbound.tax;

public interface IssueNfceUseCase {

	NfceIssuanceResult execute(IssueNfceCommand command);
}
