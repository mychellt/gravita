package br.gravita.core.ports.inbound.finance;

import br.gravita.core.domain.finance.InternalCashBoxId;
import java.time.LocalDate;
import java.util.Objects;

/** {@code account} is the internal cash box to close; both fields are required. */
public record CloseDailyCashCommand(InternalCashBoxId account, LocalDate date) {

	public CloseDailyCashCommand {
		Objects.requireNonNull(account, "account is required");
		Objects.requireNonNull(date, "date is required");
	}
}
