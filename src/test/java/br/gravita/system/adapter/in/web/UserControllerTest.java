package br.gravita.system.adapter.in.web;

import br.gravita.adapters.configuration.web.WebMvcConfiguration;
import br.gravita.adapters.inbound.controllers.tax.RegisterUserRequest;
import br.gravita.adapters.inbound.controllers.tax.UpdateUserRequest;
import br.gravita.adapters.inbound.controllers.tax.UserController;
import br.gravita.core.ports.outbound.security.SessionStorePort;
import br.gravita.core.usercases.system.ListUsersUseCase;
import br.gravita.core.usercases.system.RegisterUserCommand;
import br.gravita.core.usercases.system.RegisterUserUseCase;
import br.gravita.core.usercases.system.UpdateUserCommand;
import br.gravita.core.usercases.system.UpdateUserUseCase;
import br.gravita.core.usercases.system.UserSummary;
import br.gravita.core.domain.system.UnknownProfileException;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.domain.system.UserNotFoundException;
import br.gravita.core.domain.system.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(WebMvcConfiguration.class)
class UserControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private RegisterUserUseCase registerUserUseCase;

	@MockitoBean
	private UpdateUserUseCase updateUserUseCase;

	@MockitoBean
	private ListUsersUseCase listUsersUseCase;

	@MockitoBean
	private SessionStorePort sessionStorePort;

	private static final String SESSION_TOKEN = "session-token";
	private static final String BEARER = "Bearer " + SESSION_TOKEN;

	private final UserId callerId = UserId.generate();

	@BeforeEach
	void authenticate() {
		when(sessionStorePort.resolve(SESSION_TOKEN)).thenReturn(Optional.of(callerId));
	}

	@Test
	@DisplayName("Responds 201 with a Location header when user registration succeeds")
	void shouldReturn201WithLocationWhenRegistrationSucceeds() throws Exception {
		final UUID profileId = UUID.randomUUID();
		final UserId createdId = UserId.generate();
		final RegisterUserRequest request = new RegisterUserRequest("Jane Doe", "jane@example.com", "s3cret!", profileId);
		when(registerUserUseCase.execute(any())).thenReturn(createdId);

		mockMvc.perform(post("/api/users")
						.header("Authorization", BEARER)
						.contentType("application/json")
						.content(objectMapper.writeValueAsBytes(request)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(createdId.value().toString()));

		final ArgumentCaptor<RegisterUserCommand> captor = ArgumentCaptor.forClass(RegisterUserCommand.class);
		verify(registerUserUseCase).execute(captor.capture());
		assertThat(captor.getValue().name()).isEqualTo("Jane Doe");
		assertThat(captor.getValue().email()).isEqualTo("jane@example.com");
		assertThat(captor.getValue().rawPassword()).isEqualTo("s3cret!");
		assertThat(captor.getValue().profileId()).isEqualTo(profileId);
		assertThat(captor.getValue().callerId()).isEqualTo(callerId);
	}

	@Test
	@DisplayName("Ignores a client-supplied companyId on registration and uses the caller's session")
	void shouldIgnoreClientSuppliedCompanyIdOnRegistration() throws Exception {
		final UUID profileId = UUID.randomUUID();
		when(registerUserUseCase.execute(any())).thenReturn(UserId.generate());
		final String body = """
				{"name":"Jane Doe","email":"jane@example.com","password":"s3cret!","profileId":"%s","companyId":"%s"}"""
				.formatted(profileId, UUID.randomUUID());

		mockMvc.perform(post("/api/users").header("Authorization", BEARER).contentType("application/json").content(body))
				.andExpect(status().isCreated());

		final ArgumentCaptor<RegisterUserCommand> captor = ArgumentCaptor.forClass(RegisterUserCommand.class);
		verify(registerUserUseCase).execute(captor.capture());
		assertThat(captor.getValue().callerId()).isEqualTo(callerId);
	}

	@Test
	@DisplayName("Responds 401 when registering a user without a session")
	void shouldReturn401WhenRegisteringWithoutSession() throws Exception {
		final RegisterUserRequest request = new RegisterUserRequest("Jane Doe", "jane@example.com", "s3cret!", UUID.randomUUID());

		mockMvc.perform(post("/api/users").contentType("application/json").content(objectMapper.writeValueAsBytes(request)))
				.andExpect(status().isUnauthorized());

		verifyNoInteractions(registerUserUseCase);
	}

	@Test
	@DisplayName("Lists the users of the caller's company")
	void shouldListTheUsersOfTheCallersCompany() throws Exception {
		final UUID userId = UUID.randomUUID();
		final UUID profileId = UUID.randomUUID();
		when(listUsersUseCase.execute(callerId)).thenReturn(List.of(
				new UserSummary(userId, "Jane Doe", "jane@example.com", profileId, "Salesperson", false, UserStatus.ACTIVE)));

		mockMvc.perform(get("/api/users").header("Authorization", BEARER))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].id").value(userId.toString()))
				.andExpect(jsonPath("$[0].name").value("Jane Doe"))
				.andExpect(jsonPath("$[0].email").value("jane@example.com"))
				.andExpect(jsonPath("$[0].profileId").value(profileId.toString()))
				.andExpect(jsonPath("$[0].profileName").value("Salesperson"))
				.andExpect(jsonPath("$[0].twoFactorEnabled").value(false))
				.andExpect(jsonPath("$[0].status").value("ACTIVE"))
				.andExpect(jsonPath("$[0].password").doesNotExist())
				.andExpect(jsonPath("$[0].rawPassword").doesNotExist());
	}

	@Test
	@DisplayName("Does not accept a company id to list users: only the session decides the company")
	void shouldNotLetTheClientChooseTheCompanyWhenListing() throws Exception {
		when(listUsersUseCase.execute(callerId)).thenReturn(List.of());

		mockMvc.perform(get("/api/users").param("companyId", UUID.randomUUID().toString()).header("Authorization", BEARER))
				.andExpect(status().isOk());

		verify(listUsersUseCase).execute(callerId);
	}

	@Test
	@DisplayName("Responds 401 when listing users without a session")
	void shouldReturn401WhenListingWithoutSession() throws Exception {
		mockMvc.perform(get("/api/users")).andExpect(status().isUnauthorized());

		verifyNoInteractions(listUsersUseCase);
	}

	@Test
	@DisplayName("Responds 400 when the email is blank")
	void shouldReturn400WhenEmailIsBlank() throws Exception {
		final String body = objectMapper.writeValueAsString(
				new RegisterUserRequest("Jane Doe", " ", "s3cret!", UUID.randomUUID()));

		mockMvc.perform(post("/api/users").header("Authorization", BEARER).contentType("application/json").content(body))
				.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("Responds 400 when the profile is unknown")
	void shouldReturn400WhenProfileIsUnknown() throws Exception {
		final RegisterUserRequest request = new RegisterUserRequest("Jane Doe", "jane@example.com", "s3cret!", UUID.randomUUID());
		doThrow(new UnknownProfileException(request.profileId())).when(registerUserUseCase).execute(any());

		mockMvc.perform(post("/api/users")
						.header("Authorization", BEARER)
						.contentType("application/json")
						.content(objectMapper.writeValueAsBytes(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Unknown profile")));
	}

	@Test
	@DisplayName("Responds 204 when the user update succeeds")
	void shouldReturn204WhenUpdateSucceeds() throws Exception {
		final UUID userId = UUID.randomUUID();
		final UUID profileId = UUID.randomUUID();
		final UpdateUserRequest request = new UpdateUserRequest("Jane Roe", "jane.roe@example.com", profileId, UserStatus.INACTIVE);

		mockMvc.perform(patch("/api/users/{id}", userId)
						.contentType("application/json")
						.content(objectMapper.writeValueAsBytes(request)))
				.andExpect(status().isNoContent());

		final ArgumentCaptor<UpdateUserCommand> captor = ArgumentCaptor.forClass(UpdateUserCommand.class);
		verify(updateUserUseCase).execute(captor.capture());
		assertThat(captor.getValue().userId()).isEqualTo(userId);
		assertThat(captor.getValue().name()).isEqualTo("Jane Roe");
		assertThat(captor.getValue().email()).isEqualTo("jane.roe@example.com");
		assertThat(captor.getValue().profileId()).isEqualTo(profileId);
		assertThat(captor.getValue().status()).isEqualTo(UserStatus.INACTIVE);
	}

	@Test
	@DisplayName("Responds 404 when the user to update is unknown")
	void shouldReturn404WhenUserIsUnknown() throws Exception {
		final UUID userId = UUID.randomUUID();
		final UpdateUserRequest request = new UpdateUserRequest("Jane Roe", null, null, null);
		doThrow(new UserNotFoundException(userId)).when(updateUserUseCase).execute(any());

		mockMvc.perform(patch("/api/users/{id}", userId)
						.contentType("application/json")
						.content(objectMapper.writeValueAsBytes(request)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString(userId.toString())));
	}

	@Test
	@DisplayName("Responds 400 when the updated email is invalid")
	void shouldReturn400WhenUpdateEmailIsInvalid() throws Exception {
		final UUID userId = UUID.randomUUID();
		final String body = objectMapper.writeValueAsString(new UpdateUserRequest(null, "not-an-email", null, null));

		mockMvc.perform(patch("/api/users/{id}", userId).contentType("application/json").content(body))
				.andExpect(status().isBadRequest());
	}
}
