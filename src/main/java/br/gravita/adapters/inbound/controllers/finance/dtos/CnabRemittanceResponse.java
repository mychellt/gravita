package br.gravita.adapters.inbound.controllers.finance.dtos;

import br.gravita.core.domain.finance.BankIntegration;
import br.gravita.core.domain.finance.CnabRemittance;
import br.gravita.core.domain.finance.PayableId;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CnabRemittanceResponse(String reference, BankIntegration bankIntegration, List<UUID> payableIds,
		BigDecimal totalAmount, String fileContent) {

	public static CnabRemittanceResponse from(CnabRemittance remittance) {
		return new CnabRemittanceResponse(remittance.reference(), remittance.bankIntegration(),
				remittance.payableIds().stream().map(PayableId::value).toList(), remittance.totalAmount(),
				remittance.fileContent());
	}
}
