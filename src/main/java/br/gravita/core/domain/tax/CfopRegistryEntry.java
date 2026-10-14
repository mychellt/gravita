package br.gravita.core.domain.tax;

/** One row of the CFOP registry: the default {@link Cfop} for a {@link NaturezaOperacao}, with the label the UI shows. */
public record CfopRegistryEntry(NaturezaOperacao naturezaOperacao, Cfop cfop, String description) {
}
