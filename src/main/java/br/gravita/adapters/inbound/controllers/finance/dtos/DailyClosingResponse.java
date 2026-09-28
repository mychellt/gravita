package br.gravita.adapters.inbound.controllers.finance.dtos;

import br.gravita.core.domain.finance.DailyClosing;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record DailyClosingResponse(UUID account, LocalDate date, BigDecimal openingBalance, BigDecimal entries,
		BigDecimal exits, BigDecimal closingBalance, List<CashMovementResponse> movements) {

	public static DailyClosingResponse from(DailyClosing closing) {
		return new DailyClosingResponse(closing.getCashBoxId().value(), closing.getDate(),
				closing.getOpeningBalance(), closing.getEntries(), closing.getExits(), closing.getClosingBalance(),
				closing.getMovements().stream().map(CashMovementResponse::from).toList());
	}
}
