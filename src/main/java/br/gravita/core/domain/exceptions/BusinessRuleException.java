package br.gravita.core.domain.exceptions;

public class BusinessRuleException extends RuntimeException {
	public BusinessRuleException(final String message) {
		super(message);
	}
}
