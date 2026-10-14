package br.gravita.adapters.inbound.controllers.tax;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gravita.adapters.configuration.web.WebMvcConfiguration;
import br.gravita.adapters.inbound.controllers.tax.controllers.CfopRegistryController;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.domain.tax.Cfop;
import br.gravita.core.domain.tax.CfopRegistryEntry;
import br.gravita.core.domain.tax.NaturezaOperacao;
import br.gravita.core.ports.inbound.tax.ListCfopRegistryUseCase;
import br.gravita.core.ports.outbound.security.SessionStorePort;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CfopRegistryController.class)
@Import(WebMvcConfiguration.class)
class CfopRegistryControllerTest {

	private static final String SESSION_TOKEN = "session-token";
	private static final String BEARER = "Bearer " + SESSION_TOKEN;

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ListCfopRegistryUseCase listCfopRegistryUseCase;
	@MockitoBean
	private SessionStorePort sessionStorePort;

	@BeforeEach
	void authenticate() {
		when(sessionStorePort.resolve(SESSION_TOKEN)).thenReturn(Optional.of(UserId.generate()));
	}

	@Test
	@DisplayName("GET /api/auxiliary/cfop-registry lists natureza, CFOP, label and whether a referenced access key is required")
	void shouldListTheRegistry() throws Exception {
		when(listCfopRegistryUseCase.execute(any())).thenReturn(List.of(
				new CfopRegistryEntry(NaturezaOperacao.VENDA, new Cfop("5102"), "Venda de mercadoria"),
				new CfopRegistryEntry(NaturezaOperacao.DEVOLUCAO, new Cfop("5202"), "Devolução")));

		mockMvc.perform(get("/api/auxiliary/cfop-registry").header("Authorization", BEARER))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].naturezaOperacao").value("VENDA"))
				.andExpect(jsonPath("$[0].cfop").value("5102"))
				.andExpect(jsonPath("$[0].description").value("Venda de mercadoria"))
				.andExpect(jsonPath("$[0].requiresReferencedAccessKey").value(false))
				.andExpect(jsonPath("$[1].naturezaOperacao").value("DEVOLUCAO"))
				.andExpect(jsonPath("$[1].requiresReferencedAccessKey").value(true));
	}

	@Test
	@DisplayName("The registry is read-only: no create, update or delete endpoint exists")
	void shouldNotExposeWriteEndpoints() throws Exception {
		mockMvc.perform(post("/api/auxiliary/cfop-registry").header("Authorization", BEARER).contentType("application/json").content("{}"))
				.andExpect(status().is4xxClientError());
		mockMvc.perform(put("/api/auxiliary/cfop-registry/VENDA").header("Authorization", BEARER).contentType("application/json").content("{}"))
				.andExpect(status().is4xxClientError());
		mockMvc.perform(delete("/api/auxiliary/cfop-registry/VENDA").header("Authorization", BEARER))
				.andExpect(status().is4xxClientError());
	}
}
