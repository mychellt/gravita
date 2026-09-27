package br.gravita.core.ports.outbound.tax;

/**
 * Validates the supervisor password required by UC-M3-07 cancellations
 * (module spec: "Validates the supervisor password for cancellations").
 */
public interface SupervisorAuthorizationPort {

	boolean authorize(String supervisorCredential);
}
