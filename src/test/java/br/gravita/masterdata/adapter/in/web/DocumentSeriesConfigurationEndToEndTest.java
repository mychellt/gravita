package br.gravita.masterdata.adapter.in.web;

import br.gravita.adapters.inbound.controllers.masterdata.dtos.ConfigureDocumentSeriesRequest;
import br.gravita.adapters.inbound.controllers.masterdata.dtos.RegisterCompanyRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GRA-51: end-to-end QA verification of GRA-20's ConfigureDocumentSeriesUseCase
 * (PUT /api/companies/{id}/document-series/{type}) through the real HTTP stack -
 * real controller, real use case, real H2-backed repository. This closes the gap
 * left by DocumentSeriesConfigurationIntegrationTest (which drove the use case
 * directly because the application context previously failed to start) and by
 * CompanyControllerTest (which mocks the use case). GRA-54 fixed the
 * TotpVerificationAdapter ambiguous-constructor bug that blocked context startup,
 * so this now exercises the full path end to end.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class DocumentSeriesConfigurationEndToEndTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void fullLifecycleAgainstTheRealStack() throws Exception {
		UUID companyId = registerCompany();

		mockMvc.perform(put("/api/companies/" + companyId + "/document-series/nfe")
						.contentType("application/json")
						.content(objectMapper.writeValueAsBytes(new ConfigureDocumentSeriesRequest("001", 100L))))
				.andExpect(status().isNoContent());

		mockMvc.perform(put("/api/companies/" + companyId + "/document-series/nfe")
						.contentType("application/json")
						.content(objectMapper.writeValueAsBytes(new ConfigureDocumentSeriesRequest("001", 150L))))
				.andExpect(status().isNoContent());

		mockMvc.perform(put("/api/companies/" + companyId + "/document-series/nfe")
						.contentType("application/json")
						.content(objectMapper.writeValueAsBytes(new ConfigureDocumentSeriesRequest("001", 50L))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(containsString("nextNumber cannot be decreased")));
	}

	@Test
	void returns404ForAnUnregisteredCompany() throws Exception {
		mockMvc.perform(put("/api/companies/" + UUID.randomUUID() + "/document-series/nfe")
						.contentType("application/json")
						.content(objectMapper.writeValueAsBytes(new ConfigureDocumentSeriesRequest("001", 1L))))
				.andExpect(status().isNotFound());
	}

	@Test
	void returns400ForAnInvalidDocumentType() throws Exception {
		UUID companyId = registerCompany();

		mockMvc.perform(put("/api/companies/" + companyId + "/document-series/invalid")
						.contentType("application/json")
						.content(objectMapper.writeValueAsBytes(new ConfigureDocumentSeriesRequest("001", 1L))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(containsString("invalid")));
	}

	@Test
	void returns400WhenNextNumberIsMissing() throws Exception {
		UUID companyId = registerCompany();
		String body = "{\"series\":\"001\"}";

		mockMvc.perform(put("/api/companies/" + companyId + "/document-series/nfce")
						.contentType("application/json")
						.content(body))
				.andExpect(status().isBadRequest());
	}

	private UUID registerCompany() throws Exception {
		RegisterCompanyRequest request = new RegisterCompanyRequest(
				"11222333000181",
				"123456789",
				"987654",
				"6201-5/01",
				br.gravita.core.domain.masterdata.TaxRegime.SIMPLES_NACIONAL,
				true,
				"Rua Teste, 100",
				"nfe@example.com",
				"11999999999",
				null,
				null);

		String responseBody = mockMvc.perform(post("/api/companies")
						.contentType("application/json")
						.content(objectMapper.writeValueAsBytes(request)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();

		return UUID.fromString(objectMapper.readTree(responseBody).get("id").asString());
	}
}
