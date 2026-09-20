package br.gravita.adapters.outbound.integration;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.PersonLookupResult;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.ports.integration.CnpjLookupPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;

import static java.util.Optional.ofNullable;

@Slf4j
@Component
public class BrasilApiCnpjLookupAdapter implements CnpjLookupPort {

    private final RestClient restClient;

    public BrasilApiCnpjLookupAdapter(@Qualifier("cnpjLookupRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public Optional<PersonLookupResult> execute(final Context context) {
        final var cnpj = context.getData(Document.class);
        try {
            final var response = restClient.get()
                    .uri("/cnpj/v1/{cnpj}", cnpj.number())
                    .retrieve()
                    .body(BrasilApiCnpjResponse.class);
            return ofNullable(response).map(BrasilApiCnpjResponse::getDomain);
        } catch (RestClientException e) {
            log.warn("CNPJ lookup failed for {}: {}", cnpj.formatted(), e.getMessage());
            return Optional.empty();
        }
    }
}
