package br.gravita.adapters.outbound.security;

import br.gravita.adapters.outbound.persistence.entities.tax.UserJpaEntity;
import br.gravita.adapters.outbound.persistence.repositories.tax.UserJpaRepository;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.domain.system.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the RFC 6238 TOTP algorithm against RFC 4226 Appendix D's published HOTP(secret,
 * counter) test vectors for counter=1 (code {@code 287082}), using the well-known ASCII secret
 * {@code "12345678901234567890"} base32-encoded as {@code GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ"}.
 * The fixed clock lands at epoch second 59, so {@code 59 / 30 = 1} matches that counter.
 */
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
	void shouldAcceptTheCodeMatchingTheCurrentTimeWindow() {
		UUID userId = seedUserWithSecret(RFC4226_SECRET_BASE32);
		Clock fixedClock = Clock.fixed(Instant.ofEpochSecond(59), ZoneOffset.UTC);
		TotpVerificationAdapter adapter = new TotpVerificationAdapter(userJpaRepository, fixedClock);

		assertThat(adapter.verify(UserId.of(userId), RFC4226_COUNTER_1_CODE)).isTrue();
	}

	@Test
	void shouldRejectAnIncorrectCode() {
		UUID userId = seedUserWithSecret(RFC4226_SECRET_BASE32);
		Clock fixedClock = Clock.fixed(Instant.ofEpochSecond(59), ZoneOffset.UTC);
		TotpVerificationAdapter adapter = new TotpVerificationAdapter(userJpaRepository, fixedClock);

		assertThat(adapter.verify(UserId.of(userId), "000000")).isFalse();
	}

	@Test
	void shouldRejectWhenNoSecretIsProvisioned() {
		UUID userId = seedUserWithSecret(null);
		Clock fixedClock = Clock.fixed(Instant.ofEpochSecond(59), ZoneOffset.UTC);
		TotpVerificationAdapter adapter = new TotpVerificationAdapter(userJpaRepository, fixedClock);

		assertThat(adapter.verify(UserId.of(userId), RFC4226_COUNTER_1_CODE)).isFalse();
	}

	@Test
	void shouldRejectWhenUserIsUnknown() {
		Clock fixedClock = Clock.fixed(Instant.ofEpochSecond(59), ZoneOffset.UTC);
		TotpVerificationAdapter adapter = new TotpVerificationAdapter(userJpaRepository, fixedClock);

		assertThat(adapter.verify(UserId.generate(), RFC4226_COUNTER_1_CODE)).isFalse();
	}
}
