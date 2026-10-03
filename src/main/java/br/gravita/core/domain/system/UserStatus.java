package br.gravita.core.domain.system;

public enum UserStatus {
	ACTIVE,
	/** Deactivated by an administrator. */
	INACTIVE,
	/** Self-service signup that has not yet confirmed its e-mail address; cannot log in. */
	PENDING_ACTIVATION
}
