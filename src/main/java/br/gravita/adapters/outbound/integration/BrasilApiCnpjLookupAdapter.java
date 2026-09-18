package br.gravita.adapters.outbound.integration;

import br.gravita.core.domain.DocumentDomain;
import br.gravita.core.domain.PersonLookupResult;
import br.gravita.core.ports.integration.CnpjLookupPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;

/**
 * Fronts the Receita Federal CNPJ registry via BrasilAPI, which proxies and caches it.
 */
@Component
public class BrasilApiCnpjLookupAdapter implements CnpjLookupPort {

	private static final Logger log = LoggerFactory.getLogger(BrasilApiCnpjLookupAdapter.class);

	private final RestClient restClient;

	public BrasilApiCnpjLookupAdapter(@Qualifier("cnpjLookupRestClient") RestClient restClient) {
		this.restClient = restClient;
	}

	@Override
	public Optional<PersonLookupResult> lookup(DocumentDomain cnpj) {
		try {
			BrasilApiCnpjResponse response = restClient.get()
					.uri("/cnpj/v1/{cnpj}", cnpj.number())
					.retrieve()
					.body(BrasilApiCnpjResponse.class);
			return Optional.ofNullable(response).map(BrasilApiCnpjResponse::toDomain);
		} catch (RestClientException e) {
			log.warn("CNPJ lookup failed for {}: {}", cnpj.formatted(), e.getMessage());
			return Optional.empty();
		}
	}
}
