package br.gravita.adapters.dtos.response;

import br.gravita.core.domain.AccountType;
import br.gravita.core.domain.ChartOfAccountsDomain;

import java.util.UUID;

public record ChartOfAccountsResponse(UUID id, String code, String name, AccountType accountType, UUID parentId) {

	public static ChartOfAccountsResponse from(ChartOfAccountsDomain domain) {
		return new ChartOfAccountsResponse(domain.getId(), domain.getCode(), domain.getName(), domain.getAccountType(), domain.getParentId());
	}
}
