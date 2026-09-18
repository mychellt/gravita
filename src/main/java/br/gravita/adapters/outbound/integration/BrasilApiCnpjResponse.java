package br.gravita.adapters.outbound.integration;

import br.gravita.core.domain.AddressDomain;
import br.gravita.core.domain.PersonLookupResult;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
record BrasilApiCnpjResponse(
		@JsonProperty("razao_social") String razaoSocial,
		@JsonProperty("nome_fantasia") String nomeFantasia,
		String logradouro,
		String numero,
		String complemento,
		String bairro,
		String municipio,
		String uf,
		String cep) {

	PersonLookupResult toDomain() {
		String name = (razaoSocial != null && !razaoSocial.isBlank()) ? razaoSocial : nomeFantasia;
		AddressDomain address = AddressDomain.builder()
				.street(logradouro)
				.number(numero)
				.complement(complemento)
				.neighborhood(bairro)
				.city(municipio)
				.state(uf)
				.zipCode(cep)
				.build();
		return new PersonLookupResult(name, address);
	}
}
