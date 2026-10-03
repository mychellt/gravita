package br.gravita.core.ports.messaging;

import java.time.Instant;
import java.util.Objects;

/** What the mail adapter needs to build the activation e-mail; the link itself is the adapter's concern. */
public record ActivationEmailRequest(String to, String recipientName, String activationToken, Instant expiresAt) {

	public ActivationEmailRequest {
		Objects.requireNonNull(to, "to");
		Objects.requireNonNull(recipientName, "recipientName");
		Objects.requireNonNull(activationToken, "activationToken");
		Objects.requireNonNull(expiresAt, "expiresAt");
	}
}
