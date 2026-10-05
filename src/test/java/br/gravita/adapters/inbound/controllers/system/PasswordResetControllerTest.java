package br.gravita.adapters.inbound.controllers.system;

import br.gravita.core.domain.system.PasswordResetRejectedException;
import br.gravita.core.domain.system.PasswordResetRejectedException.Reason;
import br.gravita.core.usercases.system.ConfirmPasswordResetUseCase;
import br.gravita.core.usercases.system.RequestPasswordResetUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PasswordResetController.class)
class PasswordResetControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private RequestPasswordResetUseCase requestPasswordResetUseCase;

	@MockitoBean
	private ConfirmPasswordResetUseCase confirmPasswordResetUseCase;

	private String request(String email) throws Exception {
		return mockMvc.perform(post("/api/auth/password-reset").contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"" + email + "\"}"))
				.andExpect(status().isAccepted())
				.andReturn().getResponse().getContentAsString();
	}

	private org.springframework.test.web.servlet.ResultActions confirm(String token, String password) throws Exception {
		return mockMvc.perform(post("/api/auth/password-reset/confirm").contentType(MediaType.APPLICATION_JSON)
				.content("{\"token\":" + (token == null ? "null" : "\"" + token + "\"")
						+ ",\"newPassword\":\"" + password + "\"}"));
	}

	@Test
	@DisplayName("Request answers 202 and delegates the e-mail to the use case")
	void shouldAcceptAndDelegate() throws Exception {
		request("ana@acme.com");

		verify(requestPasswordResetUseCase).execute("ana@acme.com");
	}

	@Test
	@DisplayName("AC8: the response is byte-identical whatever the use case did with the e-mail")
	void shouldAnswerIdenticallyForEveryOutcome() throws Exception {
		// The use case returns nothing and never throws for unknown/pending/inactive/cooldown, so these calls model
		// "active user", "no such user" and "pending user"; the body must not depend on any of them.
		String active = request("active@acme.com");
		String unknown = request("ghost@acme.com");
		String pending = request("pending@acme.com");

		assertThat(unknown).isEqualTo(active).isEqualTo(pending);
		assertThat(active).contains("Se este e-mail tiver uma conta, enviaremos um link de redefinição em instantes.");
	}

	@Test
	@DisplayName("A missing or malformed e-mail is a 400 and never reaches the use case")
	void shouldRejectAnInvalidEmail() throws Exception {
		mockMvc.perform(post("/api/auth/password-reset").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(post("/api/auth/password-reset").contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"not-an-email\"}"))
				.andExpect(status().isBadRequest());

		verify(requestPasswordResetUseCase, never()).execute(any());
	}

	@Test
	@DisplayName("AC10: confirming with a good token answers 200 and delegates token and new password")
	void shouldConfirm() throws Exception {
		confirm("tok-123", "n3w-pass").andExpect(status().isOk());

		verify(confirmPasswordResetUseCase).execute("tok-123", "n3w-pass");
	}

	@Test
	@DisplayName("AC10: confirming never hands out a session or token in its body")
	void shouldNotIssueASession() throws Exception {
		final var body = confirm("tok-123", "n3w-pass").andReturn().getResponse().getContentAsString();

		assertThat(body).doesNotContain("token").doesNotContain("accessToken").doesNotContain("n3w-pass");
	}

	@Test
	@DisplayName("AC11: an expired link is 410 with reason EXPIRED, so the page can tell it from an invalid one")
	void shouldAnswerGoneForAnExpiredLink() throws Exception {
		doThrow(new PasswordResetRejectedException(Reason.EXPIRED, "Este link de redefinição expirou."))
				.when(confirmPasswordResetUseCase).execute(any(), any());

		confirm("tok-123", "n3w-pass").andExpect(status().isGone()).andExpect(jsonPath("$.reason").value("EXPIRED"));
	}

	@Test
	@DisplayName("AC11: an already used link is 410 with reason ALREADY_USED")
	void shouldAnswerGoneForAUsedLink() throws Exception {
		doThrow(new PasswordResetRejectedException(Reason.ALREADY_USED, "Este link de redefinição já foi utilizado."))
				.when(confirmPasswordResetUseCase).execute(any(), any());

		confirm("tok-123", "n3w-pass").andExpect(status().isGone())
				.andExpect(jsonPath("$.reason").value("ALREADY_USED"));
	}

	@Test
	@DisplayName("AC11: an unknown or missing link is 400 with reason INVALID")
	void shouldAnswerBadRequestForAnInvalidLink() throws Exception {
		doThrow(new PasswordResetRejectedException(Reason.INVALID, "Link de redefinição inválido."))
				.when(confirmPasswordResetUseCase).execute(any(), any());

		confirm("nope", "n3w-pass").andExpect(status().isBadRequest()).andExpect(jsonPath("$.reason").value("INVALID"));
		confirm(null, "n3w-pass").andExpect(status().isBadRequest()).andExpect(jsonPath("$.reason").value("INVALID"));
	}

	@Test
	@DisplayName("A blank new password is a 400 without a link reason and never reaches the use case")
	void shouldRejectABlankPassword() throws Exception {
		confirm("tok-123", " ").andExpect(status().isBadRequest()).andExpect(jsonPath("$.reason").doesNotExist());

		verify(confirmPasswordResetUseCase, never()).execute(any(), any());
	}

	@Test
	@DisplayName("AC12: a rejected body never echoes the password back")
	void shouldNotEchoTheRejectedPassword() throws Exception {
		final var tooLong = "x".repeat(129);

		final var body = confirm("tok-123", tooLong).andExpect(status().isBadRequest())
				.andReturn().getResponse().getContentAsString();

		assertThat(body).doesNotContain(tooLong).doesNotContain("tok-123");
		verify(confirmPasswordResetUseCase, never()).execute(any(), any());
	}
}
