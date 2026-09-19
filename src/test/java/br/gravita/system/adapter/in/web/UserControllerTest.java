package br.gravita.system.adapter.in.web;

import br.gravita.system.application.port.in.RegisterUserCommand;
import br.gravita.system.application.port.in.RegisterUserUseCase;
import br.gravita.system.application.port.in.UpdateUserCommand;
import br.gravita.system.application.port.in.UpdateUserUseCase;
import br.gravita.system.domain.model.UnknownProfileException;
import br.gravita.system.domain.model.UserId;
import br.gravita.system.domain.model.UserNotFoundException;
import br.gravita.system.domain.model.UserStatus;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private RegisterUserUseCase registerUserUseCase;

	@MockitoBean
	private UpdateUserUseCase updateUserUseCase;

	@Test
	void shouldReturn201WithLocationWhenRegistrationSucceeds() throws Exception {
		UUID profileId = UUID.randomUUID();
		UserId createdId = UserId.generate();
		RegisterUserRequest request = new RegisterUserRequest("Jane Doe", "jane@example.com", "s3cret!", profileId);
		when(registerUserUseCase.execute(any())).thenReturn(createdId);

		mockMvc.perform(post("/api/users")
						.contentType("application/json")
						.content(objectMapper.writeValueAsBytes(request)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(createdId.value().toString()));

		ArgumentCaptor<RegisterUserCommand> captor = ArgumentCaptor.forClass(RegisterUserCommand.class);
		verify(registerUserUseCase).execute(captor.capture());
		assertThat(captor.getValue().name()).isEqualTo("Jane Doe");
		assertThat(captor.getValue().email()).isEqualTo("jane@example.com");
		assertThat(captor.getValue().rawPassword()).isEqualTo("s3cret!");
		assertThat(captor.getValue().profileId()).isEqualTo(profileId);
	}

	@Test
	void shouldReturn400WhenEmailIsBlank() throws Exception {
		String body = objectMapper.writeValueAsString(
				new RegisterUserRequest("Jane Doe", " ", "s3cret!", UUID.randomUUID()));

		mockMvc.perform(post("/api/users").contentType("application/json").content(body))
				.andExpect(status().isBadRequest());
	}

	@Test
	void shouldReturn400WhenProfileIsUnknown() throws Exception {
		RegisterUserRequest request = new RegisterUserRequest("Jane Doe", "jane@example.com", "s3cret!", UUID.randomUUID());
		doThrow(new UnknownProfileException(request.profileId())).when(registerUserUseCase).execute(any());

		mockMvc.perform(post("/api/users")
						.contentType("application/json")
						.content(objectMapper.writeValueAsBytes(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Unknown profile")));
	}

	@Test
	void shouldReturn204WhenUpdateSucceeds() throws Exception {
		UUID userId = UUID.randomUUID();
		UUID profileId = UUID.randomUUID();
		UpdateUserRequest request = new UpdateUserRequest("Jane Roe", "jane.roe@example.com", profileId, UserStatus.INACTIVE);

		mockMvc.perform(patch("/api/users/{id}", userId)
						.contentType("application/json")
						.content(objectMapper.writeValueAsBytes(request)))
				.andExpect(status().isNoContent());

		ArgumentCaptor<UpdateUserCommand> captor = ArgumentCaptor.forClass(UpdateUserCommand.class);
		verify(updateUserUseCase).execute(captor.capture());
		assertThat(captor.getValue().userId()).isEqualTo(userId);
		assertThat(captor.getValue().name()).isEqualTo("Jane Roe");
		assertThat(captor.getValue().email()).isEqualTo("jane.roe@example.com");
		assertThat(captor.getValue().profileId()).isEqualTo(profileId);
		assertThat(captor.getValue().status()).isEqualTo(UserStatus.INACTIVE);
	}

	@Test
	void shouldReturn404WhenUserIsUnknown() throws Exception {
		UUID userId = UUID.randomUUID();
		UpdateUserRequest request = new UpdateUserRequest("Jane Roe", null, null, null);
		doThrow(new UserNotFoundException(userId)).when(updateUserUseCase).execute(any());

		mockMvc.perform(patch("/api/users/{id}", userId)
						.contentType("application/json")
						.content(objectMapper.writeValueAsBytes(request)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString(userId.toString())));
	}

	@Test
	void shouldReturn400WhenUpdateEmailIsInvalid() throws Exception {
		UUID userId = UUID.randomUUID();
		String body = objectMapper.writeValueAsString(new UpdateUserRequest(null, "not-an-email", null, null));

		mockMvc.perform(patch("/api/users/{id}", userId).contentType("application/json").content(body))
				.andExpect(status().isBadRequest());
	}
}
