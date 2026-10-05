package br.gravita.adapters.inbound.controllers;

import br.gravita.adapters.configuration.web.WebMvcConfiguration;
import br.gravita.core.domain.PermissionAction;
import br.gravita.core.domain.PermissionDomain;
import br.gravita.core.domain.ProfileDomain;
import br.gravita.core.domain.exceptions.DuplicateResourceException;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.system.ProfileReference;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.ports.outbound.security.SessionStorePort;
import br.gravita.core.usercases.system.ListProfilesUseCase;
import br.gravita.core.ports.business.AssignProfilePort;
import br.gravita.core.ports.business.FindProfilePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProfileRestController.class)
@Import(WebMvcConfiguration.class)
class ProfileRestControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private AssignProfilePort assignProfilePort;

	@MockitoBean
	private FindProfilePort findProfilePort;

	@MockitoBean
	private ListProfilesUseCase listProfilesUseCase;

	@MockitoBean
	private SessionStorePort sessionStorePort;

	private static final String SESSION_TOKEN = "session-token";
	private static final String BEARER = "Bearer " + SESSION_TOKEN;

	@BeforeEach
	void authenticate() {
		when(sessionStorePort.resolve(SESSION_TOKEN)).thenReturn(Optional.of(UserId.generate()));
	}

	@Test
	@DisplayName("Returns 200 OK with the id and name of every available profile")
	void shouldListProfilesForTheDropdown() throws Exception {
		final UUID adminId = UUID.randomUUID();
		final UUID salesId = UUID.randomUUID();
		when(listProfilesUseCase.execute()).thenReturn(List.of(
				new ProfileReference(adminId, "Administrator"), new ProfileReference(salesId, "Salesperson")));

		mockMvc.perform(get("/api/profiles").header("Authorization", BEARER))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].id").value(adminId.toString()))
				.andExpect(jsonPath("$[0].name").value("Administrator"))
				.andExpect(jsonPath("$[1].id").value(salesId.toString()))
				.andExpect(jsonPath("$[1].name").value("Salesperson"));
	}

	@Test
	@DisplayName("Returns 401 Unauthorized when listing profiles without a session")
	void shouldReturn401WhenListingProfilesWithoutSession() throws Exception {
		mockMvc.perform(get("/api/profiles")).andExpect(status().isUnauthorized());
	}

	private static final String VALID_BODY = """
			{"permissions":[{"module":"finance","screen":"invoices","action":"VIEW"}]}""";

	@Test
	@DisplayName("Returns 200 OK when permissions are assigned to a profile")
	void shouldReturn200WhenAssigningPermissions() throws Exception {
		final UUID id = UUID.randomUUID();
		final ProfileDomain updated = ProfileDomain.builder().id(id).name("Financial")
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
	@DisplayName("Returns 404 Not Found when assigning permissions to an unknown profile")
	void shouldReturn404WhenProfileUnknown() throws Exception {
		final UUID id = UUID.randomUUID();
		when(assignProfilePort.execute(any())).thenThrow(new ResourceNotFoundException("Profile not found: " + id));

		mockMvc.perform(put("/api/profiles/" + id + "/permissions")
						.contentType("application/json")
						.content(VALID_BODY))
				.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("Returns 400 Bad Request when the permission action is invalid")
	void shouldReturn400WhenActionIsInvalid() throws Exception {
		final UUID id = UUID.randomUUID();
		final String invalidBody = """
				{"permissions":[{"module":"finance","screen":"invoices","action":"FLY"}]}""";

		mockMvc.perform(put("/api/profiles/" + id + "/permissions")
						.contentType("application/json")
						.content(invalidBody))
				.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("Returns 400 Bad Request when the permissions list is missing")
	void shouldReturn400WhenPermissionsMissing() throws Exception {
		final UUID id = UUID.randomUUID();

		mockMvc.perform(put("/api/profiles/" + id + "/permissions")
						.contentType("application/json")
						.content("{\"permissions\":[]}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("Returns 200 OK when a new custom profile is saved")
	void shouldReturn200WhenSavingNewCustomProfile() throws Exception {
		final UUID id = UUID.randomUUID();
		final ProfileDomain created = ProfileDomain.builder().id(id).name("Sales Read-Only")
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
	@DisplayName("Returns 409 Conflict when the custom profile name is already in use")
	void shouldReturn409WhenCustomProfileNameAlreadyInUse() throws Exception {
		final UUID id = UUID.randomUUID();
		when(assignProfilePort.execute(any()))
				.thenThrow(new DuplicateResourceException("Profile name already in use: Financial"));

		mockMvc.perform(put("/api/profiles/" + id + "/permissions")
						.contentType("application/json")
						.content("""
								{"name":"Financial","permissions":[{"module":"finance","screen":"invoices","action":"VIEW"}]}"""))
				.andExpect(status().isConflict());
	}

	@Test
	@DisplayName("Returns 200 OK when an existing profile is found")
	void shouldReturn200WhenFindingExistingProfile() throws Exception {
		final UUID id = UUID.randomUUID();
		final ProfileDomain profile = ProfileDomain.builder().id(id).name("Financial")
				.permissions(List.of(PermissionDomain.builder().module("finance").screen("invoices")
						.action(PermissionAction.VIEW).build()))
				.build();
		when(findProfilePort.execute(any())).thenReturn(profile);

		mockMvc.perform(get("/api/profiles/" + id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Financial"));
	}

	@Test
	@DisplayName("Returns 404 Not Found when finding an unknown profile")
	void shouldReturn404WhenFindingUnknownProfile() throws Exception {
		final UUID id = UUID.randomUUID();
		when(findProfilePort.execute(any())).thenThrow(new ResourceNotFoundException("Profile not found: " + id));

		mockMvc.perform(get("/api/profiles/" + id))
				.andExpect(status().isNotFound());
	}
}
