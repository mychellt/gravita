package br.gravita.core.domain.exceptions;

public class UnauthorizedException extends RuntimeException {
	public UnauthorizedException(final String message) {
		super(message);
	}
}
