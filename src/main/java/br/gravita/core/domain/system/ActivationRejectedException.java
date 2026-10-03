package br.gravita.core.domain.system;

import br.gravita.core.domain.shared.BusinessRuleException;

/** An activation link was refused; {@code reason} tells the caller why so it can answer precisely. */
public class ActivationRejectedException extends BusinessRuleException {

	public enum Reason {
		INVALID, EXPIRED, ALREADY_USED
	}

	private final Reason reason;

	public ActivationRejectedException(Reason reason, String message) {
		super(message);
		this.reason = reason;
	}

	public Reason getReason() {
		return reason;
	}
}
