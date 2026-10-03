package br.gravita.core.domain.system;

import br.gravita.core.domain.system.ActivationRejectedException.Reason;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ActivationTokenTest {

	private static final Instant NOW = Instant.parse("2026-01-10T12:00:00Z");

	private final UserId userId = UserId.generate();

	@Test
	@DisplayName("A token expires 24 hours after issuance")
	void shouldExpireTwentyFourHoursAfterIssuance() {
		IssuedActivationToken issued = ActivationToken.issue(userId, NOW);

		assertThat(issued.token().getExpiresAt()).isEqualTo(NOW.plus(Duration.ofHours(24)));
		assertThat(issued.token().getUserId()).isEqualTo(userId.value());
		assertThat(issued.token().isUsed()).isFalse();
	}

	@Test
	@DisplayName("Only the hash of the secret is kept, and every token is different")
	void shouldKeepOnlyTheHashOfAUniqueSecret() {
		IssuedActivationToken first = ActivationToken.issue(userId, NOW);
		IssuedActivationToken second = ActivationToken.issue(userId, NOW);

		assertThat(first.rawToken()).isNotEqualTo(second.rawToken()).hasSizeGreaterThanOrEqualTo(43);
		assertThat(first.token().getTokenHash()).isNotEqualTo(first.rawToken())
				.isEqualTo(ActivationToken.hash(first.rawToken()));
	}

	@Test
	@DisplayName("Consuming a valid token marks it used")
	void shouldMarkTheTokenUsedWhenConsumed() {
		ActivationToken token = ActivationToken.issue(userId, NOW).token();

		token.consume(NOW.plusSeconds(60));

		assertThat(token.isUsed()).isTrue();
		assertThat(token.getUsedAt()).isEqualTo(NOW.plusSeconds(60));
	}

	@Test
	@DisplayName("A token can be used until the instant it expires")
	void shouldAcceptATokenJustBeforeItExpires() {
		ActivationToken token = ActivationToken.issue(userId, NOW).token();

		token.consume(NOW.plus(Duration.ofHours(24)).minusSeconds(1));

		assertThat(token.isUsed()).isTrue();
	}

	@Test
	@DisplayName("A token is expired from the instant it reaches its expiry, and stays unused")
	void shouldRejectAnExpiredToken() {
		ActivationToken token = ActivationToken.issue(userId, NOW).token();

		assertThatThrownBy(() -> token.consume(NOW.plus(Duration.ofHours(24))))
				.isInstanceOfSatisfying(ActivationRejectedException.class,
						e -> assertThat(e.getReason()).isEqualTo(Reason.EXPIRED));
		assertThat(token.isUsed()).isFalse();
	}

	@Test
	@DisplayName("A token is single-use: replaying it is rejected and keeps the first use time")
	void shouldRejectAReplayedToken() {
		ActivationToken token = ActivationToken.issue(userId, NOW).token();
		token.consume(NOW.plusSeconds(10));

		assertThatThrownBy(() -> token.consume(NOW.plusSeconds(20)))
				.isInstanceOfSatisfying(ActivationRejectedException.class,
						e -> assertThat(e.getReason()).isEqualTo(Reason.ALREADY_USED));
		assertThat(token.getUsedAt()).isEqualTo(NOW.plusSeconds(10));
	}

	@Test
	@DisplayName("A used token reports as used even after it has expired")
	void shouldReportUsedBeforeExpired() {
		ActivationToken token = ActivationToken.issue(userId, NOW).token();
		token.consume(NOW.plusSeconds(10));

		assertThatThrownBy(() -> token.consume(NOW.plus(Duration.ofDays(2))))
				.isInstanceOfSatisfying(ActivationRejectedException.class,
						e -> assertThat(e.getReason()).isEqualTo(Reason.ALREADY_USED));
	}
}
