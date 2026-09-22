package br.gravita.core.domain.exceptions;

public class UnauthorizedException extends RuntimeException {
	public UnauthorizedException(String message) {
		super(message);
	}
}
