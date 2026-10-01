package br.gravita.adapters.outbound.integration;

import br.gravita.core.domain.AddressDomain;
import br.gravita.core.domain.Context;
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

class ViaCepLookupAdapterTest {

	@Test
	@DisplayName("Returns the address when the ViaCEP lookup succeeds")
	void shouldReturnAddressOnSuccess() {
		RestClient.Builder builder = RestClient.builder().baseUrl("http://viacep.test/ws");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo("http://viacep.test/ws/20000000/json/"))
				.andRespond(withSuccess("""
						{
						  "cep": "20000-000",
						  "logradouro": "Rua B",
						  "bairro": "Bairro",
						  "localidade": "Rio de Janeiro",
						  "uf": "RJ"
						}
						""", MediaType.APPLICATION_JSON));
		ViaCepLookupAdapter adapter = new ViaCepLookupAdapter(builder.build());

		Optional<AddressDomain> result = adapter.execute(new Context("20000-000"));

		assertThat(result).isPresent();
		assertThat(result.get().getCity()).isEqualTo("Rio de Janeiro");
	}

	@Test
	@DisplayName("Degrades to an empty result when the CEP is not found")
	void shouldDegradeToEmptyWhenCepNotFound() {
		RestClient.Builder builder = RestClient.builder().baseUrl("http://viacep.test/ws");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo("http://viacep.test/ws/99999999/json/"))
				.andRespond(withSuccess("""
						{ "erro": true }
						""", MediaType.APPLICATION_JSON));
		ViaCepLookupAdapter adapter = new ViaCepLookupAdapter(builder.build());

		Optional<AddressDomain> result = adapter.execute(new Context("99999999"));

		assertThat(result).isEmpty();
	}

	@Test
	@DisplayName("Degrades to an empty result when the ViaCEP service fails")
	void shouldDegradeToEmptyWhenServiceFails() {
		RestClient.Builder builder = RestClient.builder().baseUrl("http://viacep.test/ws");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo("http://viacep.test/ws/20000000/json/"))
				.andRespond(withServerError());
		ViaCepLookupAdapter adapter = new ViaCepLookupAdapter(builder.build());

		Optional<AddressDomain> result = adapter.execute(new Context("20000000"));

		assertThat(result).isEmpty();
	}
}
