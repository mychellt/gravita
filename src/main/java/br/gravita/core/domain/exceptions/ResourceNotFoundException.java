package br.gravita.core.domain.exceptions;

public class ResourceNotFoundException extends RuntimeException {
	public ResourceNotFoundException(final String message) {
		super(message);
	}
}
