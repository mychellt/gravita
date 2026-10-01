package br.gravita.core.domain.tax;

/**
 * The municipality's webservice could not be reached or did not answer in time. Not a decided outcome: the transmission
 * rolls back, the document stays {@code DRAFT} and the caller may simply try again.
 */
public class NfseMunicipalityUnavailableException extends RuntimeException {

	public NfseMunicipalityUnavailableException(String message, Throwable cause) {
		super(message, cause);
	}
}
