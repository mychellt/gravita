package br.gravita.adapters.outbound.integration;

import br.gravita.core.domain.AddressDomain;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
record ViaCepResponse(
        String cep,
        String logradouro,
        String complemento,
        String bairro,
        String localidade,
        String uf,
        Boolean erro) {

    boolean notFound() {
        return Boolean.TRUE.equals(erro);
    }

    AddressDomain getDomain() {
        return AddressDomain.builder()
                .street(logradouro)
                .complement(complemento)
                .neighborhood(bairro)
                .city(localidade)
                .state(uf)
                .zipCode(cep)
                .build();
    }
}
