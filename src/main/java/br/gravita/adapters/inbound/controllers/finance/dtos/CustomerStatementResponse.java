package br.gravita.adapters.inbound.controllers.finance.dtos;

import br.gravita.core.domain.finance.CustomerStatement;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CustomerStatementResponse(UUID customerId, LocalDate from, LocalDate to,
		List<ReceivableResponse> titles, List<SettlementResponse> settlements,
		List<RenegotiationResponse> renegotiations, BigDecimal openBalance) {

	public static CustomerStatementResponse from(CustomerStatement statement) {
		return new CustomerStatementResponse(statement.customerId(), statement.from(), statement.to(),
				statement.titles().stream().map(ReceivableResponse::from).toList(),
				statement.settlements().stream().map(SettlementResponse::from).toList(),
				statement.renegotiations().stream().map(RenegotiationResponse::from).toList(),
				statement.openBalance());
	}
}
