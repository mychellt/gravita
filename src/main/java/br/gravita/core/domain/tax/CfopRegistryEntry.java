package br.gravita.core.domain.tax;

import lombok.Builder;

@Builder
public record CfopRegistryEntry(NaturezaOperacao naturezaOperacao, Cfop cfop, String description) {

}
