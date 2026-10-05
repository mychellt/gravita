package br.gravita.system.adapter.in.web;

import br.gravita.adapters.inbound.controllers.tax.ConfigureIntegrationCredentialRequest;
import br.gravita.adapters.inbound.controllers.tax.IntegrationCredentialController;
import br.gravita.core.usercases.system.ConfigureIntegrationCredentialCommand;
import br.gravita.core.usercases.system.ConfigureIntegrationCredentialUseCase;
import br.gravita.core.domain.system.IntegrationEnvironment;
import br.gravita.core.domain.system.UnknownIntegrationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(IntegrationCredentialController.class)
class IntegrationCredentialControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private ConfigureIntegrationCredentialUseCase configureIntegrationCredentialUseCase;

	@Test
	@DisplayName("Responds 204 when configuring an integration credential succeeds")
	void shouldReturn204WhenConfiguringCredentialSucceeds() throws Exception {
		final ConfigureIntegrationCredentialRequest request = new ConfigureIntegrationCredentialRequest(
				IntegrationEnvironment.PRODUCTION, "https://nfe.fazenda.example.com", "cert-payload");

		mockMvc.perform(put("/api/system/integrations/sefaz/credentials")
						.contentType("application/json")
						.content(objectMapper.writeValueAsBytes(request)))
				.andExpect(status().isNoContent());

		final ArgumentCaptor<ConfigureIntegrationCredentialCommand> captor =
				ArgumentCaptor.forClass(ConfigureIntegrationCredentialCommand.class);
		verify(configureIntegrationCredentialUseCase).execute(captor.capture());
		assertThat(captor.getValue().integrationName()).isEqualTo("sefaz");
		assertThat(captor.getValue().environment()).isEqualTo(IntegrationEnvironment.PRODUCTION);
		assertThat(captor.getValue().endpoint()).isEqualTo("https://nfe.fazenda.example.com");
		assertThat(captor.getValue().credentialPayload()).isEqualTo("cert-payload");
	}

	@Test
	@DisplayName("Responds 400 when the integration name is unknown")
	void shouldReturn400WhenIntegrationNameIsUnknown() throws Exception {
		final ConfigureIntegrationCredentialRequest request = new ConfigureIntegrationCredentialRequest(
				null, "https://stripe.example.com", "secret");
		doThrow(new UnknownIntegrationException("stripe"))
				.when(configureIntegrationCredentialUseCase).execute(org.mockito.ArgumentMatchers.any());

		mockMvc.perform(put("/api/system/integrations/stripe/credentials")
						.contentType("application/json")
						.content(objectMapper.writeValueAsBytes(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("stripe")));
	}

	@Test
	@DisplayName("Responds 400 when the endpoint is blank")
	void shouldReturn400WhenEndpointIsBlank() throws Exception {
		final String body = objectMapper.writeValueAsString(new ConfigureIntegrationCredentialRequest(null, " ", "secret"));

		mockMvc.perform(put("/api/system/integrations/bank/credentials")
						.contentType("application/json")
						.content(body))
				.andExpect(status().isBadRequest());
	}
}
