package br.gravita.adapters.inbound.controllers.finance.dtos;

import br.gravita.core.domain.finance.BankIntegration;
import br.gravita.core.domain.finance.Boleto;
import br.gravita.core.domain.finance.BoletoStatus;
import java.util.UUID;

public record BoletoResponse(UUID id, UUID receivableId, BankIntegration bankIntegration, String barcodeLine,
		BoletoStatus status) {

	public static BoletoResponse from(final Boleto boleto) {
		return new BoletoResponse(boleto.getId().value(), boleto.getReceivableId().value(),
				boleto.getBankIntegration(), boleto.getBarcodeLine(), boleto.getStatus());
	}
}
