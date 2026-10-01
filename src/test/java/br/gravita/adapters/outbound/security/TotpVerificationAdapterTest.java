package br.gravita.adapters.outbound.security;

import br.gravita.adapters.outbound.persistence.entities.tax.UserJpaEntity;
import br.gravita.adapters.outbound.persistence.repositories.tax.UserJpaRepository;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.domain.system.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TotpVerificationAdapterTest {

	private static final String RFC4226_SECRET_BASE32 = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";
	private static final String RFC4226_COUNTER_1_CODE = "287082";

	@Autowired
	private UserJpaRepository userJpaRepository;

	private UUID seedUserWithSecret(String secret) {
		UserJpaEntity entity = UserJpaEntity.builder()
				.id(UUID.randomUUID())
				.name("Admin User")
				.email("admin@example.com")
				.passwordHash("$2a$10$irrelevantForThisTest")
				.profileId(UUID.randomUUID())
				.twoFactorEnabled(true)
				.status(UserStatus.ACTIVE)
				.totpSecret(secret)
				.build();
		return userJpaRepository.save(entity).getId();
	}

	@Test
	@DisplayName("Accepts the code matching the current time window")
	void shouldAcceptTheCodeMatchingTheCurrentTimeWindow() {
		UUID userId = seedUserWithSecret(RFC4226_SECRET_BASE32);
		Clock fixedClock = Clock.fixed(Instant.ofEpochSecond(59), ZoneOffset.UTC);
		TotpVerificationAdapter adapter = new TotpVerificationAdapter(userJpaRepository, fixedClock);

		assertThat(adapter.verify(UserId.of(userId), RFC4226_COUNTER_1_CODE)).isTrue();
	}

	@Test
	@DisplayName("Rejects an incorrect code")
	void shouldRejectAnIncorrectCode() {
		UUID userId = seedUserWithSecret(RFC4226_SECRET_BASE32);
		Clock fixedClock = Clock.fixed(Instant.ofEpochSecond(59), ZoneOffset.UTC);
		TotpVerificationAdapter adapter = new TotpVerificationAdapter(userJpaRepository, fixedClock);

		assertThat(adapter.verify(UserId.of(userId), "000000")).isFalse();
	}

	@Test
	@DisplayName("Rejects verification when no secret is provisioned")
	void shouldRejectWhenNoSecretIsProvisioned() {
		UUID userId = seedUserWithSecret(null);
		Clock fixedClock = Clock.fixed(Instant.ofEpochSecond(59), ZoneOffset.UTC);
		TotpVerificationAdapter adapter = new TotpVerificationAdapter(userJpaRepository, fixedClock);

		assertThat(adapter.verify(UserId.of(userId), RFC4226_COUNTER_1_CODE)).isFalse();
	}

	@Test
	@DisplayName("Rejects verification when the user is unknown")
	void shouldRejectWhenUserIsUnknown() {
		Clock fixedClock = Clock.fixed(Instant.ofEpochSecond(59), ZoneOffset.UTC);
		TotpVerificationAdapter adapter = new TotpVerificationAdapter(userJpaRepository, fixedClock);

		assertThat(adapter.verify(UserId.generate(), RFC4226_COUNTER_1_CODE)).isFalse();
	}
}
