package br.gravita.adapters.outbound.integration;

import br.gravita.core.domain.AddressDomain;
import br.gravita.core.domain.PersonLookupResult;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
record BrasilApiCnpjResponse(
        @JsonProperty("razao_social") String companyName,
        @JsonProperty("nome_fantasia") String tradeName,
        @JsonProperty("logradouro") String street,
        @JsonProperty("numero") String number,
        @JsonProperty("complemento") String additionalAddress,
        @JsonProperty("bairro") String neighborhood,
        @JsonProperty("municipio") String city,
        @JsonProperty("uf") String state,
        @JsonProperty("cep") String zipCode) {

    PersonLookupResult getDomain() {
        final String name = (companyName != null && !companyName.isBlank()) ? companyName : tradeName;
        final AddressDomain address = AddressDomain.builder()
                .street(street)
                .number(number)
                .complement(additionalAddress)
                .neighborhood(neighborhood)
                .city(city)
                .state(state)
                .zipCode(zipCode)
                .build();
        return new PersonLookupResult(name, address);
    }
}
