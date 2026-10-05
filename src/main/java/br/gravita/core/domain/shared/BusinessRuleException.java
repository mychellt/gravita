package br.gravita.core.domain.shared;

public class BusinessRuleException extends RuntimeException {
	public BusinessRuleException(final String message) {
		super(message);
	}

	public BusinessRuleException(final String message, final Throwable cause) {
		super(message, cause);
	}
}
