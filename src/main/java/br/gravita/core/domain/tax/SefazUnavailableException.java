package br.gravita.core.domain.tax;

/**
 * Thrown by {@link br.gravita.core.ports.outbound.tax.SubmitToSefazPort} when
 * SEFAZ-UF can't be reached (timeout, connection failure, 5xx). UC-M3-04's
 * AC2: this is caught by {@code IssueNfceService} to route the sale into
 * contingency instead of blocking the cashier - it is never a 5xx surfaced
 * to the REST caller.
 */
public class SefazUnavailableException extends RuntimeException {

	public SefazUnavailableException(String message, Throwable cause) {
		super(message, cause);
	}
}
