package br.gravita.adapters.inbound.controllers.finance.dtos;

import br.gravita.core.domain.finance.InternalCashBoxId;
import br.gravita.core.ports.inbound.finance.CloseDailyCashCommand;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

/** {@code account} is optional: the back office has a single cash box, which is the default. */
public record CloseDailyCashRequest(UUID account, @NotNull LocalDate date) {

	public CloseDailyCashCommand toCommand() {
		InternalCashBoxId cashBoxId = account == null ? InternalCashBoxId.MAIN : InternalCashBoxId.of(account);
		return new CloseDailyCashCommand(cashBoxId, date);
	}
}
