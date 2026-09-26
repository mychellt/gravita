package br.gravita.core.ports.inbound.tax;

import java.util.Objects;
import java.util.UUID;

public record IssueNfceCommand(UUID nfceSaleId) {

	public IssueNfceCommand {
		Objects.requireNonNull(nfceSaleId, "nfceSaleId is required");
	}
}
