package br.gravita.adapters.dtos.request;

import br.gravita.core.domain.PaymentMethodDomain;
import br.gravita.core.domain.PaymentMethodType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record PaymentMethodRequest(@NotBlank String name, @NotNull PaymentMethodType type) {

	public PaymentMethodDomain toDomain(UUID id) {
		return PaymentMethodDomain.builder()
				.id(id)
				.name(name)
				.type(type)
				.build();
	}
}
