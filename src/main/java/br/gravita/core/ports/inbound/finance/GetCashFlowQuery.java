package br.gravita.core.ports.inbound.finance;

import br.gravita.core.domain.finance.CashFlowFilter;
import br.gravita.core.domain.finance.CashFlowGranularity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * {@code companyId}, {@code branchId}, {@code bankAccountId} and
 * {@code costCenterId} narrow the view; each may be {@code null} (no
 * restriction). {@code from} and {@code to} bound it (both inclusive) and
 * default to 30 days back and 90 days ahead of today; {@code openingBalance}
 * is the balance the running balance starts from and defaults to zero.
 */
public record GetCashFlowQuery(CashFlowGranularity granularity, UUID companyId, UUID branchId, UUID bankAccountId,
		UUID costCenterId, LocalDate from, LocalDate to, BigDecimal openingBalance) {

	public GetCashFlowQuery {
		Objects.requireNonNull(granularity, "granularity is required");
	}

	public CashFlowFilter filter() {
		return new CashFlowFilter(companyId, branchId, bankAccountId, costCenterId);
	}
}
