package br.gravita.core.domain.finance;

import java.util.UUID;

/**
 * Where a title sits in the company's books: the {@code companyId} and
 * {@code branchId} that own it and the {@code bankAccountId} it is collected
 * into (receivable) or paid from (payable). Every component is optional; a
 * title without a scope is {@link #NONE}.
 */
public record LedgerScope(UUID companyId, UUID branchId, UUID bankAccountId) {

	public static final LedgerScope NONE = new LedgerScope(null, null, null);
}
