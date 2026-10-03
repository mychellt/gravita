package br.gravita.core.domain.system;

import java.util.Objects;

/** A user still waiting for activation asked for a new activation e-mail. */
public record ActivationEmailRequested(UserId userId, String name, String email) {

	public ActivationEmailRequested {
		Objects.requireNonNull(userId, "userId");
		Objects.requireNonNull(name, "name");
		Objects.requireNonNull(email, "email");
	}
}
