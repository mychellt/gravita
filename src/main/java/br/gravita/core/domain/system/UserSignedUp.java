package br.gravita.core.domain.system;

import java.util.Objects;

/** A self-service signup was completed and its user is waiting for activation. */
public record UserSignedUp(UserId userId, String name, String email) {

	public UserSignedUp {
		Objects.requireNonNull(userId, "userId");
		Objects.requireNonNull(name, "name");
		Objects.requireNonNull(email, "email");
	}
}
