package br.gravita.core.domain.tax;

import java.util.Objects;
import java.util.UUID;

public record CashClosingReportId(UUID value) {

	public CashClosingReportId {
		Objects.requireNonNull(value, "CashClosingReportId value is required");
	}

	public static CashClosingReportId of(final UUID value) {
		return new CashClosingReportId(value);
	}
}
