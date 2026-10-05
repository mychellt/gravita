package br.gravita.adapters.dtos.request;

import br.gravita.core.domain.AccountType;
import br.gravita.core.domain.ChartOfAccountsDomain;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ChartOfAccountsRequest(@NotBlank String code, @NotBlank String name, @NotNull AccountType accountType, UUID parentId) {

	public ChartOfAccountsDomain toDomain(final UUID id) {
		return ChartOfAccountsDomain.builder()
				.id(id)
				.code(code)
				.name(name)
				.accountType(accountType)
				.parentId(parentId)
				.build();
	}
}
