package br.gravita.system.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gravita.core.domain.system.ActivationToken;
import br.gravita.core.domain.system.IssuedActivationToken;
import br.gravita.core.domain.system.ProfileReference;
import br.gravita.core.domain.system.User;
import br.gravita.core.domain.system.UserStatus;
import br.gravita.core.ports.outbound.persistence.system.ActivationTokenRepositoryPort;
import br.gravita.core.ports.outbound.persistence.system.ProfileRepositoryPort;
import br.gravita.core.ports.outbound.persistence.system.UserRepositoryPort;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class ActivateAccountEndToEndTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepositoryPort userRepositoryPort;

	@Autowired
	private ProfileRepositoryPort profileRepositoryPort;

	@Autowired
	private ActivationTokenRepositoryPort activationTokenRepositoryPort;

	private User pendingUser;

	@BeforeEach
	void persistPendingUser() {
		ProfileReference administrator = profileRepositoryPort.findByName("Administrator")
				.orElseGet(() -> new ProfileReference(UUID.randomUUID(), "Administrator"));
		pendingUser = userRepositoryPort.save(User.signUp("Ana Souza", "ana-" + UUID.randomUUID() + "@acme.test",
				"s3cret-pass", administrator, null));
	}

	private String issueTokenAt(Instant issuedAt) {
		IssuedActivationToken issued = ActivationToken.issue(pendingUser.getId(), issuedAt);
		activationTokenRepositoryPort.save(issued.token());
		return issued.rawToken();
	}

	private ResultActions activate(String token) throws Exception {
		return mockMvc.perform(get("/api/activate").param("token", token));
	}

	private UserStatus storedStatus() {
		return userRepositoryPort.findById(pendingUser.getId()).orElseThrow().getStatus();
	}

	private ActivationToken storedToken(String rawToken) {
		return activationTokenRepositoryPort.findByTokenHashForUpdate(ActivationToken.hash(rawToken)).orElseThrow();
	}

	@Test
	@DisplayName("AC4: a valid, unexpired, unused token activates the user and is marked used")
	void activatesTheUserAndSpendsTheToken() throws Exception {
		String token = issueTokenAt(Instant.now());

		activate(token).andExpect(status().isOk());

		assertThat(storedStatus()).isEqualTo(UserStatus.ACTIVE);
		assertThat(storedToken(token).isUsed()).isTrue();
	}

	@Test
	@DisplayName("AC5: replaying the same token is rejected with 410 and the user stays active")
	void rejectsAReplayedToken() throws Exception {
		String token = issueTokenAt(Instant.now());
		activate(token).andExpect(status().isOk());
		Instant firstUse = storedToken(token).getUsedAt();

		activate(token)
				.andExpect(status().isGone())
				.andExpect(jsonPath("$.reason").value("ALREADY_USED"));

		assertThat(storedStatus()).isEqualTo(UserStatus.ACTIVE);
		assertThat(storedToken(token).getUsedAt()).isEqualTo(firstUse);
	}

	@Test
	@DisplayName("AC6: an expired token is rejected cleanly with 410, the user is not activated and the token stays unused")
	void rejectsAnExpiredToken() throws Exception {
		String token = issueTokenAt(Instant.now().minus(Duration.ofHours(25)));

		activate(token)
				.andExpect(status().isGone())
				.andExpect(jsonPath("$.reason").value("EXPIRED"));

		assertThat(storedStatus()).isEqualTo(UserStatus.PENDING_ACTIVATION);
		assertThat(storedToken(token).isUsed()).isFalse();
	}

	@Test
	@DisplayName("A token that is still inside its 24 hours is accepted")
	void acceptsATokenIssuedAlmostTwentyFourHoursAgo() throws Exception {
		String token = issueTokenAt(Instant.now().minus(Duration.ofHours(23)).minus(Duration.ofMinutes(59)));

		activate(token).andExpect(status().isOk());

		assertThat(storedStatus()).isEqualTo(UserStatus.ACTIVE);
	}

	@Test
	@DisplayName("An unknown token is rejected with 400, not a 500, and activates nobody")
	void rejectsAnUnknownToken() throws Exception {
		issueTokenAt(Instant.now());

		activate("definitely-not-a-real-token")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.reason").value("INVALID"));

		assertThat(storedStatus()).isEqualTo(UserStatus.PENDING_ACTIVATION);
	}

	@Test
	@DisplayName("A request without a token gets the same clean 400 rejection")
	void rejectsAMissingToken() throws Exception {
		mockMvc.perform(get("/api/activate"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.reason").value("INVALID"));
	}

	@Test
	@DisplayName("The endpoint is public: no credentials are needed")
	void isPublic() throws Exception {
		String token = issueTokenAt(Instant.now());

		activate(token).andExpect(status().isOk());
	}
}
