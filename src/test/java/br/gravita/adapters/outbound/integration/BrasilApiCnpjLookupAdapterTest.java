package br.gravita.adapters.outbound.integration;

import br.gravita.core.domain.DocumentDomain;
import br.gravita.core.domain.PersonLookupResult;
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

	private static final DocumentDomain CNPJ = DocumentDomain.cnpj("11444777000161");

	@Test
	void shouldReturnNameAndAddressOnSuccess() {
		RestClient.Builder builder = RestClient.builder().baseUrl("http://brasilapi.test/api");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
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
		BrasilApiCnpjLookupAdapter adapter = new BrasilApiCnpjLookupAdapter(builder.build());

		Optional<PersonLookupResult> result = adapter.lookup(CNPJ);

		assertThat(result).isPresent();
		assertThat(result.get().name()).isEqualTo("Acme LTDA");
		assertThat(result.get().address().getCity()).isEqualTo("São Paulo");
	}

	@Test
	void shouldDegradeToEmptyWhenServiceFails() {
		RestClient.Builder builder = RestClient.builder().baseUrl("http://brasilapi.test/api");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo("http://brasilapi.test/api/cnpj/v1/11444777000161"))
				.andRespond(withServerError());
		BrasilApiCnpjLookupAdapter adapter = new BrasilApiCnpjLookupAdapter(builder.build());

		Optional<PersonLookupResult> result = adapter.lookup(CNPJ);

		assertThat(result).isEmpty();
	}
}
