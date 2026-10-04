package br.gravita.core.domain.exceptions;

public class MailException extends RuntimeException {

	public MailException(String message, Throwable cause) {
		super(message, cause);
	}
}
