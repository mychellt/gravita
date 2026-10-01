package br.gravita.adapters.inbound.controllers;

import br.gravita.adapters.dtos.request.PlanRequest;
import br.gravita.core.domain.PlanDomain;
import br.gravita.core.domain.PlanTier;
import br.gravita.core.ports.business.CreatePlanPort;
import br.gravita.core.ports.business.DeletePlanPort;
import br.gravita.core.ports.business.FindPlanPort;
import br.gravita.core.ports.business.ListPlansPort;
import br.gravita.core.ports.business.UpdatePlanPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PlanRestController.class)
class PlanRestControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private CreatePlanPort createPlanPort;

	@MockitoBean
	private FindPlanPort findPlanPort;

	@MockitoBean
	private ListPlansPort listPlansPort;

	@MockitoBean
	private UpdatePlanPort updatePlanPort;

	@MockitoBean
	private DeletePlanPort deletePlanPort;

	private final PlanRequest request = new PlanRequest(
			"Bronze", PlanTier.BRONZE, new BigDecimal("297"), new BigDecimal("247"), List.of("1 CNPJ · 1 filial"));

	@Test
	@DisplayName("Returns 201 Created when a plan is created")
	void shouldReturn201WhenCreatingPlan() throws Exception {
		PlanDomain created = request.toDomain(UUID.randomUUID());
		when(createPlanPort.execute(any())).thenReturn(created);

		mockMvc.perform(post("/api/plans")
						.contentType("application/json")
						.content(objectMapper.writeValueAsBytes(request)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("Bronze"));
	}

	@Test
	@DisplayName("Returns 200 OK when plans are listed")
	void shouldReturn200WhenListingPlans() throws Exception {
		when(listPlansPort.execute(any())).thenReturn(List.of(request.toDomain(UUID.randomUUID())));

		mockMvc.perform(get("/api/plans"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].name").value("Bronze"));
	}

	@Test
	@DisplayName("Returns 200 OK when an existing plan is found")
	void shouldReturn200WhenFindingExistingPlan() throws Exception {
		UUID id = UUID.randomUUID();
		when(findPlanPort.execute(any())).thenReturn(request.toDomain(id));

		mockMvc.perform(get("/api/plans/" + id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Bronze"));
	}

	@Test
	@DisplayName("Returns 200 OK when a plan is updated")
	void shouldReturn200WhenUpdatingPlan() throws Exception {
		UUID id = UUID.randomUUID();
		when(updatePlanPort.execute(any())).thenReturn(request.toDomain(id));

		mockMvc.perform(put("/api/plans/" + id)
						.contentType("application/json")
						.content(objectMapper.writeValueAsBytes(request)))
				.andExpect(status().isOk());
	}

	@Test
	@DisplayName("Returns 204 No Content when a plan is deleted")
	void shouldReturn204WhenDeletingPlan() throws Exception {
		mockMvc.perform(delete("/api/plans/" + UUID.randomUUID()))
				.andExpect(status().isNoContent());
	}
}
