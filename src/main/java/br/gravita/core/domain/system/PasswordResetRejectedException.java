package br.gravita.core.domain.system;

import br.gravita.core.domain.shared.BusinessRuleException;

/** A password reset link was refused; {@code reason} tells the caller why so it can answer precisely. */
public class PasswordResetRejectedException extends BusinessRuleException {

	public enum Reason {
		INVALID, EXPIRED, ALREADY_USED
	}

	private final Reason reason;

	public PasswordResetRejectedException(Reason reason, String message) {
		super(message);
		this.reason = reason;
	}

	public Reason getReason() {
		return reason;
	}
}
