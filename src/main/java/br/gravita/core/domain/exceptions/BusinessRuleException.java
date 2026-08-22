package br.gravita.core.domain.exceptions;

public class BusinessRuleException extends RuntimeException {
	public BusinessRuleException(String message) {
		super(message);
	}
}
