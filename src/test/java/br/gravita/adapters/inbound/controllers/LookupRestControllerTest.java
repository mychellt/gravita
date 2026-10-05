package br.gravita.adapters.inbound.controllers;

import br.gravita.core.domain.AddressDomain;
import br.gravita.core.domain.PersonLookupResult;
import br.gravita.core.ports.business.LookupPersonByDocumentPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LookupRestController.class)
class LookupRestControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private LookupPersonByDocumentPort lookupPersonByDocumentPort;

	@Test
	@DisplayName("Returns 200 with the company name and address for a valid CNPJ")
	void shouldReturn200WithNameAndAddressForValidCnpj() throws Exception {
		final AddressDomain address = AddressDomain.builder()
				.street("Rua A").number("10").neighborhood("Centro").city("São Paulo").state("SP").zipCode("01000-000")
				.build();
		when(lookupPersonByDocumentPort.execute(any())).thenReturn(new PersonLookupResult("Acme LTDA", address));

		mockMvc.perform(get("/api/lookup/cnpj/11444777000161"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Acme LTDA"))
				.andExpect(jsonPath("$.address.city").value("São Paulo"));
	}

	@Test
	@DisplayName("Returns 400 Bad Request for a malformed CNPJ")
	void shouldReturn400ForMalformedCnpj() throws Exception {
		mockMvc.perform(get("/api/lookup/cnpj/12345"))
				.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("Returns 200 with the address only when looking up a CEP")
	void shouldReturn200WithAddressOnlyForCep() throws Exception {
		final AddressDomain address = AddressDomain.builder()
				.street("Rua B").neighborhood("Bairro").city("Rio de Janeiro").state("RJ").zipCode("20000-000")
				.build();
		when(lookupPersonByDocumentPort.execute(any())).thenReturn(new PersonLookupResult(null, address));

		mockMvc.perform(get("/api/lookup/cep/20000000"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").doesNotExist())
				.andExpect(jsonPath("$.address.city").value("Rio de Janeiro"));
	}

	@Test
	@DisplayName("Returns 200 with an empty result when the lookup service degrades")
	void shouldReturn200WithEmptyResultWhenLookupDegrades() throws Exception {
		when(lookupPersonByDocumentPort.execute(any())).thenReturn(PersonLookupResult.notFound());

		mockMvc.perform(get("/api/lookup/cep/99999999"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.address").doesNotExist());
	}
}
