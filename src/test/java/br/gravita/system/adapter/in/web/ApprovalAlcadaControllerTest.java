package br.gravita.system.adapter.in.web;

import br.gravita.adapters.inbound.controllers.tax.ApprovalAlcadaController;
import br.gravita.adapters.inbound.controllers.tax.ConfigureApprovalAlcadaRequest;
import br.gravita.core.usercases.system.ConfigureApprovalAlcadaCommand;
import br.gravita.core.usercases.system.ConfigureApprovalAlcadaUseCase;
import br.gravita.core.domain.system.UnknownApprovalModuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ApprovalAlcadaController.class)
class ApprovalAlcadaControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private ConfigureApprovalAlcadaUseCase configureApprovalAlcadaUseCase;

	@Test
	@DisplayName("Responds 204 when configuring an approval alcada succeeds")
	void shouldReturn204WhenConfiguringAlcadaSucceeds() throws Exception {
		final UUID approverProfileId = UUID.randomUUID();
		final ConfigureApprovalAlcadaRequest request = new ConfigureApprovalAlcadaRequest(new BigDecimal("5000.00"), null,
				approverProfileId);

		mockMvc.perform(put("/api/system/alcadas/purchasing")
						.contentType("application/json")
						.content(objectMapper.writeValueAsBytes(request)))
				.andExpect(status().isNoContent());

		final ArgumentCaptor<ConfigureApprovalAlcadaCommand> captor =
				ArgumentCaptor.forClass(ConfigureApprovalAlcadaCommand.class);
		verify(configureApprovalAlcadaUseCase).execute(captor.capture());
		assertThat(captor.getValue().module()).isEqualTo("purchasing");
		assertThat(captor.getValue().thresholdValue()).isEqualByComparingTo("5000.00");
		assertThat(captor.getValue().approverProfileId()).isEqualTo(approverProfileId);
	}

	@Test
	@DisplayName("Responds 400 when the approval module is unknown")
	void shouldReturn400WhenModuleIsUnknown() throws Exception {
		final ConfigureApprovalAlcadaRequest request = new ConfigureApprovalAlcadaRequest(new BigDecimal("100"), null,
				UUID.randomUUID());
		doThrow(new UnknownApprovalModuleException("logistics"))
				.when(configureApprovalAlcadaUseCase).execute(ArgumentMatchers.any());

		mockMvc.perform(put("/api/system/alcadas/logistics")
						.contentType("application/json")
						.content(objectMapper.writeValueAsBytes(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(containsString("logistics")));
	}

	@Test
	@DisplayName("Responds 400 when the approver profile id is missing")
	void shouldReturn400WhenApproverProfileIdIsMissing() throws Exception {
		final String body = objectMapper.writeValueAsString(new ConfigureApprovalAlcadaRequest(new BigDecimal("100"), null,
				null));

		mockMvc.perform(put("/api/system/alcadas/finance")
						.contentType("application/json")
						.content(body))
				.andExpect(status().isBadRequest());
	}
}
