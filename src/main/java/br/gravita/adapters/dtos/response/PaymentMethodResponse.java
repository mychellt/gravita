package br.gravita.adapters.dtos.response;

import br.gravita.core.domain.PaymentMethodDomain;
import br.gravita.core.domain.PaymentMethodType;

import java.util.UUID;

public record PaymentMethodResponse(UUID id, String name, PaymentMethodType type) {

	public static PaymentMethodResponse from(final PaymentMethodDomain domain) {
		return new PaymentMethodResponse(domain.getId(), domain.getName(), domain.getType());
	}
}
