package br.gravita.core.ports.inbound.finance;

import java.util.Objects;
import java.util.UUID;

/** {@code pixChargeId} is the txid the bank echoes back from {@code PixChargeIssueRequest}. */
public record ConfirmPixPaymentCommand(UUID pixChargeId) {

	public ConfirmPixPaymentCommand {
		Objects.requireNonNull(pixChargeId, "pixChargeId is required");
	}
}
