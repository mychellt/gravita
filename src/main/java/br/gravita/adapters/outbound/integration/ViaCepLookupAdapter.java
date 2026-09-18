package br.gravita.adapters.outbound.integration;

import br.gravita.core.domain.AddressDomain;
import br.gravita.core.ports.integration.CepLookupPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;
import java.util.regex.Pattern;

@Component
public class ViaCepLookupAdapter implements CepLookupPort {

	private static final Logger log = LoggerFactory.getLogger(ViaCepLookupAdapter.class);
	private static final Pattern NON_DIGIT = Pattern.compile("\\D");

	private final RestClient restClient;

	public ViaCepLookupAdapter(@Qualifier("cepLookupRestClient") RestClient restClient) {
		this.restClient = restClient;
	}

	@Override
	public Optional<AddressDomain> lookup(String cep) {
		String digits = NON_DIGIT.matcher(cep == null ? "" : cep).replaceAll("");
		try {
			ViaCepResponse response = restClient.get()
					.uri("/{cep}/json/", digits)
					.retrieve()
					.body(ViaCepResponse.class);
			if (response == null || response.notFound()) {
				return Optional.empty();
			}
			return Optional.of(response.toDomain());
		} catch (RestClientException e) {
			log.warn("CEP lookup failed for {}: {}", digits, e.getMessage());
			return Optional.empty();
		}
	}
}
