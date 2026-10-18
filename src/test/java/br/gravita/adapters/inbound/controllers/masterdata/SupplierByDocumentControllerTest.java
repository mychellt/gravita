package br.gravita.adapters.inbound.controllers.masterdata;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gravita.adapters.configuration.web.WebMvcConfiguration;
import br.gravita.adapters.inbound.controllers.masterdata.controllers.SupplierController;
import br.gravita.core.domain.masterdata.Supplier;
import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.ports.inbound.masterdata.FindSupplierByDocumentUseCase;
import br.gravita.core.ports.inbound.masterdata.RegisterSupplierUseCase;
import br.gravita.core.ports.inbound.masterdata.UpdateSupplierUseCase;
import br.gravita.core.ports.outbound.security.SessionStorePort;
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

@WebMvcTest(SupplierController.class)
@Import(WebMvcConfiguration.class)
class SupplierByDocumentControllerTest {

	private static final String BEARER = "Bearer session-token";

	@Autowired
	private MockMvc mockMvc;
	@MockitoBean
	private FindSupplierByDocumentUseCase findSupplierByDocumentUseCase;
	@MockitoBean
	private RegisterSupplierUseCase registerSupplierUseCase;
	@MockitoBean
	private UpdateSupplierUseCase updateSupplierUseCase;
	@MockitoBean
	private SessionStorePort sessionStorePort;

	@BeforeEach
	void authenticate() {
		when(sessionStorePort.resolve("session-token")).thenReturn(Optional.of(UserId.generate()));
	}

	@Test
	@DisplayName("GET /api/suppliers/by-document/{cnpj} returns the registered supplier")
	void returnsTheRegisteredSupplier() throws Exception {
		final Supplier supplier = mock(Supplier.class);
		when(supplier.getId()).thenReturn(SupplierId.of(UUID.randomUUID()));
		when(supplier.getName()).thenReturn("Fornecedor Alfa");
		when(supplier.getDocument()).thenReturn(Document.cnpj("11222333000181"));
		when(findSupplierByDocumentUseCase.execute("11222333000181")).thenReturn(Optional.of(supplier));

		mockMvc.perform(get("/api/suppliers/by-document/11222333000181").header("Authorization", BEARER))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Fornecedor Alfa"))
				.andExpect(jsonPath("$.document").value("11222333000181"));
	}

	@Test
	@DisplayName("GET /api/suppliers/by-document/{cnpj} is 404 when no supplier has the CNPJ")
	void is404WhenUnknown() throws Exception {
		when(findSupplierByDocumentUseCase.execute("11222333000181")).thenReturn(Optional.empty());

		mockMvc.perform(get("/api/suppliers/by-document/11222333000181").header("Authorization", BEARER))
				.andExpect(status().isNotFound());
	}
}
