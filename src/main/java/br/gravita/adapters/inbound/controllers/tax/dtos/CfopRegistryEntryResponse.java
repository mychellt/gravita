package br.gravita.adapters.inbound.controllers.tax.dtos;

import br.gravita.core.domain.tax.CfopRegistryEntry;
import br.gravita.core.domain.tax.NaturezaOperacao;
import lombok.Builder;

@Builder
public record CfopRegistryEntryResponse(NaturezaOperacao naturezaOperacao, String cfop, String description,
                                        boolean requiresReferencedAccessKey) {

    public static CfopRegistryEntryResponse from(final CfopRegistryEntry entry) {
        return CfopRegistryEntryResponse.builder()
                .naturezaOperacao(entry.naturezaOperacao())
                .cfop(entry.cfop().code())
                .description(entry.description())
                .requiresReferencedAccessKey(entry.naturezaOperacao().requiresReferencedAccessKey())
                .build();
    }
}
