package br.gravita.adapters.inbound.controllers.inventory;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gravita.adapters.inbound.controllers.inventory.controllers.StockBalanceController;
import br.gravita.core.domain.ProductDomain;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.ports.inbound.inventory.GetStockBalanceUseCase;
import br.gravita.core.ports.inbound.inventory.SetAllowNegativeStockUseCase;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(StockBalanceController.class)
class StockBalanceControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private GetStockBalanceUseCase getStockBalanceUseCase;

	@MockitoBean
	private SetAllowNegativeStockUseCase setAllowNegativeStockUseCase;

	@Test
	@DisplayName("Toggling allow-negative-stock returns the updated value")
	void togglingAllowNegativeStockReturnsTheUpdatedValue() throws Exception {
		final UUID id = UUID.randomUUID();
		final ProductDomain updated = ProductDomain.builder().id(id).allowNegativeStock(true).build();
		when(setAllowNegativeStockUseCase.execute(any())).thenReturn(updated);

		mockMvc.perform(patch("/api/inventory/products/" + id + "/allow-negative-stock")
						.contentType("application/json")
						.content("{\"allowNegativeStock\": true}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.productId").value(id.toString()))
				.andExpect(jsonPath("$.allowNegativeStock").value(true));
	}

	@Test
	@DisplayName("Rejects a missing allowNegativeStock value")
	void rejectsAMissingValue() throws Exception {
		mockMvc.perform(patch("/api/inventory/products/" + UUID.randomUUID() + "/allow-negative-stock")
						.contentType("application/json")
						.content("{}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("An unknown product is a 404, not a silent no-op")
	void unknownProductIsNotFound() throws Exception {
		final UUID id = UUID.randomUUID();
		when(setAllowNegativeStockUseCase.execute(any()))
				.thenThrow(new ResourceNotFoundException("Product not found: " + id));

		mockMvc.perform(patch("/api/inventory/products/" + id + "/allow-negative-stock")
						.contentType("application/json")
						.content("{\"allowNegativeStock\": true}"))
				.andExpect(status().isNotFound());
	}
}
