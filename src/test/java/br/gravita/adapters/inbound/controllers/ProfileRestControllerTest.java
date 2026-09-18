package br.gravita.adapters.inbound.controllers;

import br.gravita.core.domain.PermissionAction;
import br.gravita.core.domain.PermissionDomain;
import br.gravita.core.domain.ProfileDomain;
import br.gravita.core.domain.exceptions.DuplicateResourceException;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.ports.business.AssignProfilePort;
import br.gravita.core.ports.business.FindProfilePort;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProfileRestController.class)
class ProfileRestControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private AssignProfilePort assignProfilePort;

	@MockitoBean
	private FindProfilePort findProfilePort;

	private static final String VALID_BODY = """
			{"permissions":[{"module":"finance","screen":"invoices","action":"VIEW"}]}""";

	@Test
	void shouldReturn200WhenAssigningPermissions() throws Exception {
		UUID id = UUID.randomUUID();
		ProfileDomain updated = ProfileDomain.builder().id(id).name("Financial")
				.permissions(List.of(PermissionDomain.builder().module("finance").screen("invoices")
						.action(PermissionAction.VIEW).build()))
				.build();
		when(assignProfilePort.execute(any())).thenReturn(updated);

		mockMvc.perform(put("/api/profiles/" + id + "/permissions")
						.contentType("application/json")
						.content(VALID_BODY))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Financial"))
				.andExpect(jsonPath("$.permissions[0].action").value("VIEW"));
	}

	@Test
	void shouldReturn404WhenProfileUnknown() throws Exception {
		UUID id = UUID.randomUUID();
		when(assignProfilePort.execute(any())).thenThrow(new ResourceNotFoundException("Profile not found: " + id));

		mockMvc.perform(put("/api/profiles/" + id + "/permissions")
						.contentType("application/json")
						.content(VALID_BODY))
				.andExpect(status().isNotFound());
	}

	@Test
	void shouldReturn400WhenActionIsInvalid() throws Exception {
		UUID id = UUID.randomUUID();
		String invalidBody = """
				{"permissions":[{"module":"finance","screen":"invoices","action":"FLY"}]}""";

		mockMvc.perform(put("/api/profiles/" + id + "/permissions")
						.contentType("application/json")
						.content(invalidBody))
				.andExpect(status().isBadRequest());
	}

	@Test
	void shouldReturn400WhenPermissionsMissing() throws Exception {
		UUID id = UUID.randomUUID();

		mockMvc.perform(put("/api/profiles/" + id + "/permissions")
						.contentType("application/json")
						.content("{\"permissions\":[]}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void shouldReturn200WhenSavingNewCustomProfile() throws Exception {
		UUID id = UUID.randomUUID();
		ProfileDomain created = ProfileDomain.builder().id(id).name("Sales Read-Only")
				.permissions(List.of(PermissionDomain.builder().module("sales").screen("orders")
						.action(PermissionAction.VIEW).build()))
				.build();
		when(assignProfilePort.execute(any())).thenReturn(created);

		mockMvc.perform(put("/api/profiles/" + id + "/permissions")
						.contentType("application/json")
						.content("""
								{"name":"Sales Read-Only","permissions":[{"module":"sales","screen":"orders","action":"VIEW"}]}"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Sales Read-Only"));
	}

	@Test
	void shouldReturn409WhenCustomProfileNameAlreadyInUse() throws Exception {
		UUID id = UUID.randomUUID();
		when(assignProfilePort.execute(any()))
				.thenThrow(new DuplicateResourceException("Profile name already in use: Financial"));

		mockMvc.perform(put("/api/profiles/" + id + "/permissions")
						.contentType("application/json")
						.content("""
								{"name":"Financial","permissions":[{"module":"finance","screen":"invoices","action":"VIEW"}]}"""))
				.andExpect(status().isConflict());
	}

	@Test
	void shouldReturn200WhenFindingExistingProfile() throws Exception {
		UUID id = UUID.randomUUID();
		ProfileDomain profile = ProfileDomain.builder().id(id).name("Financial")
				.permissions(List.of(PermissionDomain.builder().module("finance").screen("invoices")
						.action(PermissionAction.VIEW).build()))
				.build();
		when(findProfilePort.execute(any())).thenReturn(profile);

		mockMvc.perform(get("/api/profiles/" + id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Financial"));
	}

	@Test
	void shouldReturn404WhenFindingUnknownProfile() throws Exception {
		UUID id = UUID.randomUUID();
		when(findProfilePort.execute(any())).thenThrow(new ResourceNotFoundException("Profile not found: " + id));

		mockMvc.perform(get("/api/profiles/" + id))
				.andExpect(status().isNotFound());
	}
}
