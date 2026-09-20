package br.gravita.core.ports.outbound.persistence;

import java.util.UUID;

/**
 * Extension point for the finance context (M8), which owns the "is this node referenced by an
 * expense/entry?" check. M1 has no dependency on M8, so until finance exists this is backed by a
 * no-op adapter; M8's persistence adapter is expected to supply the real implementation.
 */
public interface FinanceUsageQueryPort {
	boolean isCostCenterInUse(final UUID costCenterId);
	boolean isChartOfAccountInUse(final UUID chartOfAccountId);
}
