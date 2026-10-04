package br.gravita.adapters.inbound.controllers.system;

import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.domain.system.ActivationRejectedException;
import br.gravita.core.domain.system.ActivationRejectedException.Reason;
import br.gravita.core.usercases.system.ActivateAccountUseCase;
import br.gravita.core.usercases.system.ResendActivationUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ActivationController.class)
class ActivationControllerActivateTest {

	private static final String PAGE = "http://localhost:4200/activate.html";
	private static final String TOKEN = "8Fu5TwNd4FN7jj0j1dC5g-kl9M_d6OgKgS9w2Y4YU7c";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ActivateAccountUseCase activateAccountUseCase;

	@MockitoBean
	private ResendActivationUseCase resendActivationUseCase;

	private void assertRedirectsTo(String token, String expectedStatus) throws Exception {
		var request = token == null ? get("/api/activate") : get("/api/activate").param("token", token);
		var location = mockMvc.perform(request)
				.andExpect(status().isFound())
				.andExpect(header().string("Cache-Control", "no-store"))
				.andReturn().getResponse().getHeader("Location");

		assertThat(location).isEqualTo(PAGE + "?status=" + expectedStatus);
		assertThat(location).as("the secret token must not travel on to the page").doesNotContain("token");
	}

	@Test
	@DisplayName("A valid link activates the account and redirects to the page with status=activated")
	void shouldRedirectActivated() throws Exception {
		assertRedirectsTo(TOKEN, "activated");

		verify(activateAccountUseCase).execute(TOKEN);
	}

	@Test
	@DisplayName("An expired link redirects with status=expired")
	void shouldRedirectExpired() throws Exception {
		doThrow(new ActivationRejectedException(Reason.EXPIRED, "Este link de ativação expirou."))
				.when(activateAccountUseCase).execute(TOKEN);

		assertRedirectsTo(TOKEN, "expired");
	}

	@Test
	@DisplayName("A link that was already used redirects with status=used")
	void shouldRedirectUsed() throws Exception {
		doThrow(new ActivationRejectedException(Reason.ALREADY_USED, "Este link de ativação já foi utilizado."))
				.when(activateAccountUseCase).execute(TOKEN);

		assertRedirectsTo(TOKEN, "used");
	}

	@Test
	@DisplayName("An unknown link redirects with status=invalid")
	void shouldRedirectInvalid() throws Exception {
		doThrow(new ActivationRejectedException(Reason.INVALID, "Link de ativação inválido."))
				.when(activateAccountUseCase).execute(TOKEN);

		assertRedirectsTo(TOKEN, "invalid");
	}

	@Test
	@DisplayName("A missing token lands on the same page as a bad one")
	void shouldRedirectInvalidWhenTheTokenIsMissing() throws Exception {
		doThrow(new ActivationRejectedException(Reason.INVALID, "Link de ativação inválido."))
				.when(activateAccountUseCase).execute(null);

		assertRedirectsTo(null, "invalid");
	}

	@Test
	@DisplayName("A business rule rejection (e.g. a deactivated account) is shown as an unusable link")
	void shouldRedirectInvalidOnABusinessRuleRejection() throws Exception {
		doThrow(new BusinessRuleException("A conta não está aguardando ativação."))
				.when(activateAccountUseCase).execute(TOKEN);

		assertRedirectsTo(TOKEN, "invalid");
	}

	@Test
	@DisplayName("An unexpected failure redirects with status=unavailable instead of showing an error response")
	void shouldRedirectUnavailableOnAnUnexpectedFailure() throws Exception {
		doThrow(new IllegalStateException("database down")).when(activateAccountUseCase).execute(TOKEN);

		assertRedirectsTo(TOKEN, "unavailable");
	}
}
