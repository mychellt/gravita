package br.gravita.adapters.dtos.response;

import br.gravita.core.domain.PaymentMethodType;

public record PaymentMethodTypeResponse(PaymentMethodType code, String description) {

	public static PaymentMethodTypeResponse from(final PaymentMethodType type) {
		return new PaymentMethodTypeResponse(type, type.getDescription());
	}
}
