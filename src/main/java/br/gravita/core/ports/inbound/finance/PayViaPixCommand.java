package br.gravita.core.ports.inbound.finance;

import java.util.Objects;
import java.util.UUID;

/** {@code pixKey} is the recipient's PIX key. */
public record PayViaPixCommand(UUID payableId, String pixKey) {

	public PayViaPixCommand {
		Objects.requireNonNull(payableId, "payableId is required");
		Objects.requireNonNull(pixKey, "pixKey is required");
		if (pixKey.isBlank()) {
			throw new IllegalArgumentException("pixKey must not be blank");
		}
		pixKey = pixKey.strip();
	}
}
