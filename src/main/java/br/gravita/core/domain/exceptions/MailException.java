package br.gravita.core.domain.exceptions;

public class MailException extends RuntimeException {

	public MailException(final String message, final Throwable cause) {
		super(message, cause);
	}
}
