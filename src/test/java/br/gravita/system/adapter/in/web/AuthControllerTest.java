package br.gravita.system.adapter.in.web;

import br.gravita.adapters.inbound.controllers.tax.AuthController;
import br.gravita.adapters.inbound.controllers.tax.LoginRequest;
import br.gravita.adapters.inbound.controllers.tax.TwoFactorVerifyRequest;
import br.gravita.core.usercases.system.AuthResult;
import br.gravita.core.usercases.system.AuthenticateCommand;
import br.gravita.core.usercases.system.AuthenticateUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private AuthenticateUseCase authenticateUseCase;

	@Test
	@DisplayName("Responds 200 with a session token when login succeeds")
	void shouldReturn200WithSessionTokenWhenLoginSucceeds() throws Exception {
		when(authenticateUseCase.execute(any())).thenReturn(AuthResult.authenticated("token-123"));

		mockMvc.perform(post("/api/auth/login")
						.header("User-Agent", "Chrome")
						.contentType("application/json")
						.content(objectMapper.writeValueAsBytes(new LoginRequest("jane@example.com", "s3cret!"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("AUTHENTICATED"))
				.andExpect(jsonPath("$.sessionToken").value("token-123"));

		ArgumentCaptor<AuthenticateCommand> captor = ArgumentCaptor.forClass(AuthenticateCommand.class);
		verify(authenticateUseCase).execute(captor.capture());
		assertThat(captor.getValue().email()).isEqualTo("jane@example.com");
		assertThat(captor.getValue().rawPassword()).isEqualTo("s3cret!");
		assertThat(captor.getValue().device()).isEqualTo("Chrome");
	}

	@Test
	@DisplayName("Responds 200 flagging that a TOTP code is required when two-factor is pending")
	void shouldReturn200WithTotpRequiredWhenTwoFactorIsPending() throws Exception {
		when(authenticateUseCase.execute(any())).thenReturn(AuthResult.totpRequired());

		mockMvc.perform(post("/api/auth/login")
						.contentType("application/json")
						.content(objectMapper.writeValueAsBytes(new LoginRequest("admin@example.com", "s3cret!"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("TOTP_REQUIRED"))
				.andExpect(jsonPath("$.sessionToken").doesNotExist());
	}

	@Test
	@DisplayName("Responds 401 when the login is rejected")
	void shouldReturn401WhenLoginIsRejected() throws Exception {
		when(authenticateUseCase.execute(any())).thenReturn(AuthResult.rejected());

		mockMvc.perform(post("/api/auth/login")
						.contentType("application/json")
						.content(objectMapper.writeValueAsBytes(new LoginRequest("jane@example.com", "wrong"))))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value("REJECTED"));
	}

	@Test
	@DisplayName("Responds 400 when the login email is blank")
	void shouldReturn400WhenLoginEmailIsBlank() throws Exception {
		mockMvc.perform(post("/api/auth/login")
						.contentType("application/json")
						.content(objectMapper.writeValueAsBytes(new LoginRequest(" ", "s3cret!"))))
				.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("Responds 200 as authenticated when TOTP verification succeeds")
	void shouldReturn200AndAuthenticatedWhenTotpVerificationSucceeds() throws Exception {
		when(authenticateUseCase.execute(any())).thenReturn(AuthResult.authenticated("token-456"));

		mockMvc.perform(post("/api/auth/2fa/verify")
						.contentType("application/json")
						.content(objectMapper.writeValueAsBytes(new TwoFactorVerifyRequest("admin@example.com", "s3cret!", "123456"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("AUTHENTICATED"))
				.andExpect(jsonPath("$.sessionToken").value("token-456"));

		ArgumentCaptor<AuthenticateCommand> captor = ArgumentCaptor.forClass(AuthenticateCommand.class);
		verify(authenticateUseCase).execute(captor.capture());
		assertThat(captor.getValue().totpCode()).isEqualTo("123456");
	}

	@Test
	@DisplayName("Responds 401 when the TOTP code is invalid")
	void shouldReturn401WhenTotpCodeIsInvalid() throws Exception {
		when(authenticateUseCase.execute(any())).thenReturn(AuthResult.rejected());

		mockMvc.perform(post("/api/auth/2fa/verify")
						.contentType("application/json")
						.content(objectMapper.writeValueAsBytes(new TwoFactorVerifyRequest("admin@example.com", "s3cret!", "000000"))))
				.andExpect(status().isUnauthorized());
	}

}
