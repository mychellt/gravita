package br.gravita.adapters.inbound.controllers;

import br.gravita.adapters.dtos.request.RegisterProductRequest;
import br.gravita.adapters.dtos.request.UpdateProductRequest;
import br.gravita.core.domain.ProductDomain;
import br.gravita.core.domain.ProductStatus;
import br.gravita.core.domain.ProductType;
import br.gravita.core.ports.business.RegisterProductPort;
import br.gravita.core.ports.business.UpdateProductPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductRestController.class)
class ProductRestControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private RegisterProductPort registerProductPort;

	@MockitoBean
	private UpdateProductPort updateProductPort;

	private final RegisterProductRequest request = new RegisterProductRequest(
			"SKU-1", List.of("7891234567895"), ProductType.SIMPLE, "12345678", "0100", 0,
			null, null, null, null, null, null, null, null, null, null, null, null,
			List.of("http://example.com/1.png"), null, null);

	@Test
	void shouldReturn201WhenRegisteringProduct() throws Exception {
		ProductDomain created = request.toDomain(UUID.randomUUID());
		created.setStatus(ProductStatus.ACTIVE);
		when(registerProductPort.execute(any())).thenReturn(created);

		mockMvc.perform(post("/api/products")
						.contentType("application/json")
						.content(objectMapper.writeValueAsBytes(request)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.internalCode").value("SKU-1"))
				.andExpect(jsonPath("$.status").value("ACTIVE"));
	}

	@Test
	void shouldReturn400WhenMoreThanFiveImagesAreSubmitted() throws Exception {
		RegisterProductRequest tooManyImages = new RegisterProductRequest(
				"SKU-1", List.of("7891234567895"), ProductType.SIMPLE, "12345678", "0100", 0,
				null, null, null, null, null, null, null, null, null, null, null, null,
				List.of("1", "2", "3", "4", "5", "6"), null, null);

		mockMvc.perform(post("/api/products")
						.contentType("application/json")
						.content(objectMapper.writeValueAsBytes(tooManyImages)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void shouldReturn400WhenTypeIsMissing() throws Exception {
		RegisterProductRequest missingType = new RegisterProductRequest(
				"SKU-1", null, null, null, null, null,
				null, null, null, null, null, null, null, null, null, null, null, null,
				null, null, null);

		mockMvc.perform(post("/api/products")
						.contentType("application/json")
						.content(objectMapper.writeValueAsBytes(missingType)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void shouldReturn200WhenPartiallyUpdatingProduct() throws Exception {
		UUID id = UUID.randomUUID();
		UpdateProductRequest partialUpdate = new UpdateProductRequest(
				null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
				null, null, null, ProductStatus.INACTIVE, null, null);
		ProductDomain updated = request.toDomain(id);
		updated.setStatus(ProductStatus.INACTIVE);
		when(updateProductPort.execute(any())).thenReturn(updated);

		mockMvc.perform(patch("/api/products/" + id)
						.contentType("application/json")
						.content(objectMapper.writeValueAsBytes(partialUpdate)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("INACTIVE"));
	}
}
