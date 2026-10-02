package br.gravita.adapters.inbound.controllers;

import br.gravita.adapters.dtos.request.PlanRequest;
import br.gravita.core.domain.Context;
import br.gravita.core.domain.PlanDomain;
import br.gravita.core.domain.PlanFeature;
import br.gravita.core.domain.PlanLimits;
import br.gravita.core.domain.PlanSupport;
import br.gravita.core.domain.PlanTier;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.ports.business.CreatePlanPort;
import br.gravita.core.ports.business.DeletePlanPort;
import br.gravita.core.ports.business.FindPlanPort;
import br.gravita.core.ports.business.ListPlansPort;
import br.gravita.core.ports.business.UpdatePlanPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
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
			"Bronze", "Para quem está começando", PlanTier.BRONZE, new BigDecimal("297"), new BigDecimal("247"),
			true, true,
			new PlanLimits(1, null, 2, 3),
			List.of(new PlanRequest.FeatureRequest("1 CNPJ · 1 filial", true), new PlanRequest.FeatureRequest("API", false)),
			new PlanSupport(true, false, true, 24, true, false));

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
	@DisplayName("Passes the description, limits, ordered features and support of the request to the use case")
	void shouldMapRequestToDomain() throws Exception {
		when(createPlanPort.execute(any())).thenReturn(request.toDomain(UUID.randomUUID()));

		mockMvc.perform(post("/api/plans")
						.contentType("application/json")
						.content(objectMapper.writeValueAsBytes(request)))
				.andExpect(status().isCreated());

		ArgumentCaptor<Context> captor = ArgumentCaptor.forClass(Context.class);
		verify(createPlanPort).execute(captor.capture());
		PlanDomain sent = captor.getValue().getData(PlanDomain.class);
		assertThat(sent.getDescription()).isEqualTo("Para quem está começando");
		assertThat(sent.isActivePlan()).isTrue();
		assertThat(sent.isFeatured()).isTrue();
		assertThat(sent.getLimits()).isEqualTo(new PlanLimits(1, null, 2, 3));
		assertThat(sent.getFeatures()).containsExactly(
				new PlanFeature("1 CNPJ · 1 filial", true, 0), new PlanFeature("API", false, 1));
		assertThat(sent.getSupport()).isEqualTo(new PlanSupport(true, false, true, 24, true, false));
	}

	@Test
	@DisplayName("Returns every plan field, with null limits meaning unlimited and features in display order")
	void shouldReturnFullPlanShape() throws Exception {
		PlanDomain plan = request.toDomain(UUID.randomUUID());
		plan.setFeatures(List.of(new PlanFeature("Second", false, 1), new PlanFeature("First", true, 0)));
		when(findPlanPort.execute(any())).thenReturn(plan);

		mockMvc.perform(get("/api/plans/" + plan.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.description").value("Para quem está começando"))
				.andExpect(jsonPath("$.active").value(true))
				.andExpect(jsonPath("$.featured").value(true))
				.andExpect(jsonPath("$.limits.cnpjs").value(1))
				.andExpect(jsonPath("$.limits.filiais").doesNotExist())
				.andExpect(jsonPath("$.limits.caixasPdv").value(2))
				.andExpect(jsonPath("$.limits.usuarios").value(3))
				.andExpect(jsonPath("$.features[0].label").value("First"))
				.andExpect(jsonPath("$.features[0].included").value(true))
				.andExpect(jsonPath("$.features[1].label").value("Second"))
				.andExpect(jsonPath("$.features[1].included").value(false))
				.andExpect(jsonPath("$.support.email").value(true))
				.andExpect(jsonPath("$.support.chat").value(false))
				.andExpect(jsonPath("$.support.telefone").value(true))
				.andExpect(jsonPath("$.support.slaHoras").value(24))
				.andExpect(jsonPath("$.support.horarioComercial").value(true))
				.andExpect(jsonPath("$.support.gerenteDedicado").value(false));
	}

	@Test
	@DisplayName("Returns 400 Bad Request when the description, active flag, limits, features or support are missing")
	void shouldReturn400WhenRequiredFieldsAreMissing() throws Exception {
		for (String field : List.of("description", "active", "limits", "features", "support")) {
			ObjectNode body = objectMapper.valueToTree(request);
			body.remove(field);

			mockMvc.perform(post("/api/plans")
							.contentType("application/json")
							.content(objectMapper.writeValueAsBytes(body)))
					.andExpect(status().isBadRequest());
		}
	}

	@Test
	@DisplayName("Returns 409 Conflict when a business rule rejects the plan")
	void shouldReturn409WhenBusinessRuleFails() throws Exception {
		when(createPlanPort.execute(any())).thenThrow(new BusinessRuleException("A plan named 'Bronze' already exists"));

		mockMvc.perform(post("/api/plans")
						.contentType("application/json")
						.content(objectMapper.writeValueAsBytes(request)))
				.andExpect(status().isConflict());
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

	@Test
	@DisplayName("Returns 409 Conflict when deleting a plan that has subscriptions")
	void shouldReturn409WhenDeletingPlanWithSubscriptions() throws Exception {
		when(deletePlanPort.execute(any())).thenThrow(new BusinessRuleException("Plan has subscriptions"));

		mockMvc.perform(delete("/api/plans/" + UUID.randomUUID()))
				.andExpect(status().isConflict());
	}
}
