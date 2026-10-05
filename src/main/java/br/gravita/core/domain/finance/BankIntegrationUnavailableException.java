package br.gravita.core.domain.finance;

/** The chosen bank could not issue the request (not configured, or unreachable). */
public class BankIntegrationUnavailableException extends RuntimeException {

	public BankIntegrationUnavailableException(final String message) {
		super(message);
	}

	public BankIntegrationUnavailableException(final String message, final Throwable cause) {
		super(message, cause);
	}
}
