package br.gravita.adapters.inbound.controllers.tax.dtos;

import br.gravita.core.domain.tax.CfopRegistryEntry;
import br.gravita.core.domain.tax.NaturezaOperacao;

public record CfopRegistryEntryResponse(NaturezaOperacao naturezaOperacao, String cfop, String description,
		boolean requiresReferencedAccessKey) {

	public static CfopRegistryEntryResponse from(final CfopRegistryEntry entry) {
		return new CfopRegistryEntryResponse(entry.naturezaOperacao(), entry.cfop().code(), entry.description(),
				entry.naturezaOperacao().requiresReferencedAccessKey());
	}
}
