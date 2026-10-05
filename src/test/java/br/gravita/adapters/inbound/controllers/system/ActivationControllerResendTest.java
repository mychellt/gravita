package br.gravita.adapters.inbound.controllers.system;

import br.gravita.core.usercases.system.ActivateAccountUseCase;
import br.gravita.core.usercases.system.ResendActivationUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ActivationController.class)
class ActivationControllerResendTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ActivateAccountUseCase activateAccountUseCase;

	@MockitoBean
	private ResendActivationUseCase resendActivationUseCase;

	private String resend(final String email) throws Exception {
		return mockMvc.perform(post("/api/activate/resend").contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"" + email + "\"}"))
				.andExpect(status().isAccepted())
				.andReturn().getResponse().getContentAsString();
	}

	@Test
	@DisplayName("Resend answers 202 and delegates the e-mail to the use case")
	void shouldAcceptAndDelegate() throws Exception {
		resend("ana@acme.com");

		verify(resendActivationUseCase).execute("ana@acme.com");
	}

	@Test
	@DisplayName("AC8: the response is byte-identical whatever the use case did with the e-mail")
	void shouldAnswerIdenticallyForEveryOutcome() throws Exception {
		// The use case returns nothing and never throws for unknown/active/cooldown, so these three calls model
		// "pending user", "no such user" and "already active"; the body must not depend on any of them.
		final String pending = resend("pending@acme.com");
		final String unknown = resend("ghost@acme.com");
		final String active = resend("active@acme.com");

		assertThat(unknown).isEqualTo(pending).isEqualTo(active);
		assertThat(pending).contains("Se este e-mail estiver aguardando ativação");
	}

	@Test
	@DisplayName("A missing or malformed e-mail is a 400 and never reaches the use case")
	void shouldRejectAnInvalidEmail() throws Exception {
		mockMvc.perform(post("/api/activate/resend").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(post("/api/activate/resend").contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"not-an-email\"}"))
				.andExpect(status().isBadRequest());

		verify(resendActivationUseCase, never()).execute(org.mockito.ArgumentMatchers.any());
	}
}
