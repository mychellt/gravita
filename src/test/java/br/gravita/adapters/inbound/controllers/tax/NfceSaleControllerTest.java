package br.gravita.adapters.inbound.controllers.tax;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gravita.adapters.configuration.web.WebMvcConfiguration;
import br.gravita.adapters.inbound.controllers.tax.controllers.NfceSaleController;
import br.gravita.core.domain.PaymentMethodType;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.domain.tax.NfceSaleId;
import br.gravita.core.domain.tax.NfceSaleStatus;
import br.gravita.core.ports.inbound.tax.CancelNfceUseCase;
import br.gravita.core.ports.inbound.tax.IssueNfceUseCase;
import br.gravita.core.ports.inbound.tax.NfceIssuanceResult;
import br.gravita.core.ports.inbound.tax.RegisterNfceSaleUseCase;
import br.gravita.core.ports.outbound.security.SessionStorePort;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(NfceSaleController.class)
@Import(WebMvcConfiguration.class)
class NfceSaleControllerTest {

	private static final String BEARER = "Bearer session-token";

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private RegisterNfceSaleUseCase registerNfceSaleUseCase;
	@MockitoBean
	private IssueNfceUseCase issueNfceUseCase;
	@MockitoBean
	private CancelNfceUseCase cancelNfceUseCase;
	@MockitoBean
	private SessionStorePort sessionStorePort;

	private final NfceSaleId saleId = NfceSaleId.of(UUID.randomUUID());

	@BeforeEach
	void authenticate() {
		when(sessionStorePort.resolve("session-token")).thenReturn(Optional.of(UserId.generate()));
		when(registerNfceSaleUseCase.execute(any())).thenReturn(saleId);
	}

	private String saleBody() throws Exception {
		return objectMapper.writeValueAsString(Map.of(
				"sessionId", UUID.randomUUID(),
				"items", java.util.List.of(Map.of("productId", UUID.randomUUID(), "quantity", 2, "unitPrice", 10.5)),
				"payments", java.util.List.of(Map.of("method", PaymentMethodType.CASH, "amount", 21))));
	}

	@Test
	@DisplayName("POST /api/pdv/sales stores the sale and answers with the authorization")
	void shouldRegisterAndIssue() throws Exception {
		when(issueNfceUseCase.execute(any())).thenReturn(new NfceIssuanceResult(NfceSaleStatus.AUTHORIZED, "KEY", "PROTO"));

		mockMvc.perform(post("/api/pdv/sales").header("Authorization", BEARER).contentType("application/json").content(saleBody()))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "/api/pdv/sales/" + saleId.value()))
				.andExpect(jsonPath("$.status").value("AUTHORIZED"))
				.andExpect(jsonPath("$.accessKey").value("KEY"))
				.andExpect(jsonPath("$.issuanceError").doesNotExist());
	}

	@Test
	@DisplayName("A failed issuance still answers 201 with the stored sale as DRAFT and the reason")
	void shouldKeepTheStoredSaleWhenIssuanceFails() throws Exception {
		when(issueNfceUseCase.execute(any())).thenThrow(new IllegalStateException("Company state is required"));

		mockMvc.perform(post("/api/pdv/sales").header("Authorization", BEARER).contentType("application/json").content(saleBody()))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(saleId.value().toString()))
				.andExpect(jsonPath("$.status").value("DRAFT"))
				.andExpect(jsonPath("$.accessKey").doesNotExist())
				.andExpect(jsonPath("$.issuanceError").value("Company state is required"));
	}
}
