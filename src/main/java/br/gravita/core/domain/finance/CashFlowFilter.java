package br.gravita.core.domain.finance;

import java.util.UUID;

/**
 * The dimensions a cash-flow view can be narrowed by; a {@code null} component
 * does not restrict. {@code costCenterId} only ever matches payables, as
 * receivables are not charged to a cost center.
 */
public record CashFlowFilter(UUID companyId, UUID branchId, UUID bankAccountId, UUID costCenterId) {

	public static final CashFlowFilter NONE = new CashFlowFilter(null, null, null, null);
}
