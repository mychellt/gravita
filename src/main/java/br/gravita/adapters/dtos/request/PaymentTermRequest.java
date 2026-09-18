package br.gravita.adapters.dtos.request;

import br.gravita.core.domain.PaymentTermDomain;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;
import java.util.UUID;

public record PaymentTermRequest(
		@NotBlank String name,
		@NotEmpty List<@PositiveOrZero Integer> installmentIntervalsDays) {

	public PaymentTermDomain toDomain(UUID id) {
		return PaymentTermDomain.builder()
				.id(id)
				.name(name)
				.installmentIntervalsDays(installmentIntervalsDays)
				.build();
	}
}
