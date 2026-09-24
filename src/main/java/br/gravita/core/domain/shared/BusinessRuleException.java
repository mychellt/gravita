package br.gravita.core.domain.shared;

public class BusinessRuleException extends RuntimeException {
	public BusinessRuleException(String message) {
		super(message);
	}

	public BusinessRuleException(String message, Throwable cause) {
		super(message, cause);
	}
}
