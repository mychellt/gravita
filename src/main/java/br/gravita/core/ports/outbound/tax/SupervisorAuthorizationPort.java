package br.gravita.core.ports.outbound.tax;

public interface SupervisorAuthorizationPort {

	boolean authorize(String supervisorCredential);
}
