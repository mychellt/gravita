package br.gravita.adapters.outbound.security;

import br.gravita.adapters.outbound.persistence.entities.tax.UserJpaEntity;
import br.gravita.adapters.outbound.persistence.repositories.tax.UserJpaRepository;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.domain.system.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TotpVerificationAdapterTest {

	private static final String RFC4226_SECRET_BASE32 = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";
	private static final String RFC4226_COUNTER_1_CODE = "287082";

	@Mock
	private UserJpaRepository repository;

	private TotpVerificationAdapter adapter;

	@BeforeEach
	void setUp() {
		final Clock fixedClock = Clock.fixed(Instant.ofEpochSecond(59), ZoneOffset.UTC);
		adapter = new TotpVerificationAdapter(repository, fixedClock);
	}

	@Test
	@DisplayName("Accepts the code matching the current time window")
	void shouldAcceptTheCodeMatchingTheCurrentTimeWindow() {
		final UUID userId = UUID.randomUUID();
		when(repository.findById(userId)).thenReturn(Optional.of(buildUser(userId, RFC4226_SECRET_BASE32)));

		assertThat(adapter.verify(UserId.of(userId), RFC4226_COUNTER_1_CODE)).isTrue();
		verify(repository).findById(userId);
	}

	@Test
	@DisplayName("Rejects an incorrect code")
	void shouldRejectAnIncorrectCode() {
		final UUID userId = UUID.randomUUID();
		when(repository.findById(userId)).thenReturn(Optional.of(buildUser(userId, RFC4226_SECRET_BASE32)));

		assertThat(adapter.verify(UserId.of(userId), "000000")).isFalse();
	}

	@Test
	@DisplayName("Rejects verification when no secret is provisioned")
	void shouldRejectWhenNoSecretIsProvisioned() {
		final UUID userId = UUID.randomUUID();
		when(repository.findById(userId)).thenReturn(Optional.of(buildUser(userId, null)));

		assertThat(adapter.verify(UserId.of(userId), RFC4226_COUNTER_1_CODE)).isFalse();
	}

	@Test
	@DisplayName("Rejects verification when the user is unknown")
	void shouldRejectWhenUserIsUnknown() {
		final UUID userId = UUID.randomUUID();
		when(repository.findById(userId)).thenReturn(Optional.empty());

		assertThat(adapter.verify(UserId.of(userId), RFC4226_COUNTER_1_CODE)).isFalse();
	}

	private UserJpaEntity buildUser(final UUID id, final String secret) {
		return UserJpaEntity.builder()
				.id(id)
				.name("Admin User")
				.email("admin@example.com")
				.passwordHash("$2a$10$irrelevantForThisTest")
				.profileId(UUID.randomUUID())
				.twoFactorEnabled(true)
				.status(UserStatus.ACTIVE)
				.totpSecret(secret)
				.build();
	}
}
