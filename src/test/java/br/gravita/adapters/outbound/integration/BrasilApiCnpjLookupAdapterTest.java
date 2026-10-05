package br.gravita.adapters.outbound.integration;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.PersonLookupResult;
import br.gravita.core.domain.shared.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class BrasilApiCnpjLookupAdapterTest {

	private static final Document CNPJ = Document.cnpj("11444777000161");

	@Test
	@DisplayName("Returns the company name and address when the BrasilAPI lookup succeeds")
	void shouldReturnNameAndAddressOnSuccess() {
		final RestClient.Builder builder = RestClient.builder().baseUrl("http://brasilapi.test/api");
		final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo("http://brasilapi.test/api/cnpj/v1/11444777000161"))
				.andRespond(withSuccess("""
						{
						  "razao_social": "Acme LTDA",
						  "nome_fantasia": "Acme",
						  "logradouro": "Rua A",
						  "numero": "10",
						  "bairro": "Centro",
						  "municipio": "São Paulo",
						  "uf": "SP",
						  "cep": "01000-000"
						}
						""", MediaType.APPLICATION_JSON));
		final BrasilApiCnpjLookupAdapter adapter = new BrasilApiCnpjLookupAdapter(builder.build());

		final Optional<PersonLookupResult> result = adapter.execute(new Context(CNPJ));

		assertThat(result).isPresent();
		assertThat(result.get().name()).isEqualTo("Acme LTDA");
		assertThat(result.get().address().getCity()).isEqualTo("São Paulo");
	}

	@Test
	@DisplayName("Degrades to an empty result when the BrasilAPI service fails")
	void shouldDegradeToEmptyWhenServiceFails() {
		final RestClient.Builder builder = RestClient.builder().baseUrl("http://brasilapi.test/api");
		final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo("http://brasilapi.test/api/cnpj/v1/11444777000161"))
				.andRespond(withServerError());
		final BrasilApiCnpjLookupAdapter adapter = new BrasilApiCnpjLookupAdapter(builder.build());

		final Optional<PersonLookupResult> result = adapter.execute(new Context(CNPJ));

		assertThat(result).isEmpty();
	}
}
