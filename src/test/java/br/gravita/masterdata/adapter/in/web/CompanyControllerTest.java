package br.gravita.masterdata.adapter.in.web;

import br.gravita.adapters.inbound.controllers.masterdata.controllers.CompanyController;
import br.gravita.adapters.inbound.controllers.masterdata.dtos.ConfigureDocumentSeriesRequest;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.DocumentSeriesNotFoundException;
import br.gravita.core.domain.masterdata.FiscalDocumentType;
import br.gravita.core.ports.inbound.masterdata.ConfigureDocumentSeriesCommand;
import br.gravita.core.ports.inbound.masterdata.ConfigureDocumentSeriesUseCase;
import br.gravita.core.ports.inbound.masterdata.RegisterCompanyUseCase;
import br.gravita.core.ports.inbound.masterdata.SwitchSefazEnvironmentUseCase;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CompanyController.class)
class CompanyControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private RegisterCompanyUseCase registerCompanyUseCase;

	@MockitoBean
	private SwitchSefazEnvironmentUseCase switchSefazEnvironmentUseCase;

	@MockitoBean
	private ConfigureDocumentSeriesUseCase configureDocumentSeriesUseCase;

	@Test
	void shouldReturn204WhenConfiguringDocumentSeriesSucceeds() throws Exception {
		UUID companyId = UUID.randomUUID();
		ConfigureDocumentSeriesRequest request = new ConfigureDocumentSeriesRequest("001", 1000L);

		mockMvc.perform(put("/api/companies/" + companyId + "/document-series/nfe")
						.contentType("application/json")
						.content(objectMapper.writeValueAsBytes(request)))
				.andExpect(status().isNoContent());

		ArgumentCaptor<ConfigureDocumentSeriesCommand> captor = ArgumentCaptor.forClass(ConfigureDocumentSeriesCommand.class);
		verify(configureDocumentSeriesUseCase).execute(captor.capture());
		assertThat(captor.getValue().companyId()).isEqualTo(CompanyId.of(companyId));
		assertThat(captor.getValue().documentType()).isEqualTo(FiscalDocumentType.NFE);
		assertThat(captor.getValue().series()).isEqualTo("001");
		assertThat(captor.getValue().nextNumber()).isEqualTo(1000L);
	}

	@Test
	void shouldReturn400WhenDocumentTypeIsUnknown() throws Exception {
		ConfigureDocumentSeriesRequest request = new ConfigureDocumentSeriesRequest("001", 1L);

		mockMvc.perform(put("/api/companies/" + UUID.randomUUID() + "/document-series/invalid")
						.contentType("application/json")
						.content(objectMapper.writeValueAsBytes(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(containsString("invalid")));
	}

	@Test
	void shouldReturn400WhenSeriesIsMissing() throws Exception {
		String body = objectMapper.writeValueAsString(new ConfigureDocumentSeriesRequest(null, 1L));

		mockMvc.perform(put("/api/companies/" + UUID.randomUUID() + "/document-series/nfce")
						.contentType("application/json")
						.content(body))
				.andExpect(status().isBadRequest());
	}

	@Test
	void shouldReturn404WhenCompanyOrSeriesDoesNotExist() throws Exception {
		UUID companyId = UUID.randomUUID();
		doThrow(new DocumentSeriesNotFoundException(companyId, FiscalDocumentType.NFSE))
				.when(configureDocumentSeriesUseCase).execute(ArgumentMatchers.any());

		mockMvc.perform(put("/api/companies/" + companyId + "/document-series/nfse")
						.contentType("application/json")
						.content(objectMapper.writeValueAsBytes(new ConfigureDocumentSeriesRequest("001", 1L))))
				.andExpect(status().isNotFound());
	}
}
