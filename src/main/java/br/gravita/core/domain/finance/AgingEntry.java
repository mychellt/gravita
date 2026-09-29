package br.gravita.core.domain.finance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/** An outstanding title reduced to what the aging needs: its due date and what is still owed on it. */
public record AgingEntry(LocalDate dueDate, BigDecimal outstanding) {

	public AgingEntry {
		Objects.requireNonNull(dueDate, "dueDate is required");
		Objects.requireNonNull(outstanding, "outstanding is required");
	}
}
