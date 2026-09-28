package br.gravita.core.domain.finance;

/** The chosen bank could not issue the request (not configured, or unreachable). */
public class BankIntegrationUnavailableException extends RuntimeException {

	public BankIntegrationUnavailableException(String message) {
		super(message);
	}

	public BankIntegrationUnavailableException(String message, Throwable cause) {
		super(message, cause);
	}
}
