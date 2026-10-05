package br.gravita.core.domain.exceptions;

public class DuplicateResourceException extends RuntimeException {
	public DuplicateResourceException(final String message) {
		super(message);
	}
}
