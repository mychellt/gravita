package br.gravita.adapters.outbound.integration;

import br.gravita.core.domain.AddressDomain;
import br.gravita.core.domain.Context;
import br.gravita.core.ports.integration.CepLookupPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;
import java.util.regex.Pattern;

@Slf4j
@Component
public class ViaCepLookupAdapter implements CepLookupPort {

    private static final Pattern NON_DIGIT = Pattern.compile("\\D");

    private final RestClient restClient;

    public ViaCepLookupAdapter(@Qualifier("cepLookupRestClient") final RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public Optional<AddressDomain> execute(final Context context) {
        final String cep = context.getData(String.class);
        final String digits = NON_DIGIT.matcher(cep == null ? "" : cep).replaceAll("");
        try {
            final ViaCepResponse response = restClient.get()
                    .uri("/{cep}/json/", digits)
                    .retrieve()
                    .body(ViaCepResponse.class);
            if (response == null || response.notFound()) {
                return Optional.empty();
            }
            return Optional.of(response.getDomain());
        } catch (final RestClientException e) {
            log.warn("CEP lookup failed for {}: {}", digits, e.getMessage());
            return Optional.empty();
        }
    }
}
