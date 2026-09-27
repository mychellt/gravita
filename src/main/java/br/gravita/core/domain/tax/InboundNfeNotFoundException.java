package br.gravita.core.domain.tax;

import java.util.UUID;

public class InboundNfeNotFoundException extends RuntimeException {

	public InboundNfeNotFoundException(UUID inboundNfeId) {
		super("Inbound NFe not found: " + inboundNfeId);
	}
}
