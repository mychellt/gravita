package br.gravita.adapters.dtos.response;

import br.gravita.core.domain.PaymentTermDomain;

import java.util.List;
import java.util.UUID;

public record PaymentTermResponse(UUID id, String name, List<Integer> installmentIntervalsDays, int numberOfInstallments) {

	public static PaymentTermResponse from(PaymentTermDomain domain) {
		return new PaymentTermResponse(
				domain.getId(), domain.getName(), domain.getInstallmentIntervalsDays(), domain.getNumberOfInstallments());
	}
}
