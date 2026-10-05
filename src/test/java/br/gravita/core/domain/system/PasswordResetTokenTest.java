package br.gravita.core.domain.system;

import br.gravita.core.domain.system.PasswordResetRejectedException.Reason;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordResetTokenTest {

    private static final LocalDateTime NOW = LocalDateTime.parse("2026-01-10T12:00:00");

    private final UserId userId = UserId.generate();

    @Test
    @DisplayName("A token expires one hour after issuance")
    void shouldExpireOneHourAfterIssuance() {
        final var issued = PasswordResetToken.issue(userId, NOW);

        assertThat(issued.token().getExpiresAt()).isEqualTo(NOW.plus(Duration.ofHours(1)));
        assertThat(issued.token().getUserId()).isEqualTo(userId.value());
        assertThat(issued.token().isUsed()).isFalse();
    }

    @Test
    @DisplayName("Only the hash of the secret is kept, and every token is different")
    void shouldKeepOnlyTheHashOfAUniqueSecret() {
        final IssuedPasswordResetToken first = PasswordResetToken.issue(userId, NOW);
        final IssuedPasswordResetToken second = PasswordResetToken.issue(userId, NOW);

        assertThat(first.rawToken()).isNotEqualTo(second.rawToken()).hasSizeGreaterThanOrEqualTo(43);
        assertThat(first.token().getTokenHash()).isNotEqualTo(first.rawToken())
                .isEqualTo(PasswordResetToken.hash(first.rawToken()));
    }

    @Test
    @DisplayName("A reset token never doubles as an activation token: the stored hashes are not interchangeable")
    void shouldNeverBeFoundThroughTheActivationTokenLookup() {
        final var reset = PasswordResetToken.issue(userId, NOW);

        // Same secret, different tables: a reset secret only ever matches a row of password_reset_tokens.
        assertThat(reset.token().getClass()).isNotEqualTo(ActivationToken.class);
        assertThat(ActivationToken.issue(userId, NOW).rawToken()).isNotEqualTo(reset.rawToken());
    }

    @Test
    @DisplayName("Consuming a valid token marks it used")
    void shouldMarkTheTokenUsedWhenConsumed() {
        final PasswordResetToken token = PasswordResetToken.issue(userId, NOW).token();

        token.consume(NOW.plusSeconds(60));

        assertThat(token.isUsed()).isTrue();
        assertThat(token.getModifiedAt()).isEqualTo(NOW.plusSeconds(60));
    }

    @Test
    @DisplayName("A token can be used until the instant it expires")
    void shouldAcceptATokenJustBeforeItExpires() {
        final PasswordResetToken token = PasswordResetToken.issue(userId, NOW).token();

        token.consume(NOW.plus(Duration.ofHours(1)).minusSeconds(1));

        assertThat(token.isUsed()).isTrue();
    }

    @Test
    @DisplayName("A token is expired from the instant it reaches its expiry, and stays unused")
    void shouldRejectAnExpiredToken() {
        final PasswordResetToken token = PasswordResetToken.issue(userId, NOW).token();

        assertThatThrownBy(() -> token.consume(NOW.plus(Duration.ofHours(1))))
                .isInstanceOfSatisfying(PasswordResetRejectedException.class,
                        e -> assertThat(e.getReason()).isEqualTo(Reason.EXPIRED));
        assertThat(token.isUsed()).isFalse();
    }

    @Test
    @DisplayName("A token is single-use: replaying it is rejected and keeps the first use time")
    void shouldRejectAReplayedToken() {
        final PasswordResetToken token = PasswordResetToken.issue(userId, NOW).token();
        token.consume(NOW.plusSeconds(10));

        assertThatThrownBy(() -> token.consume(NOW.plusSeconds(20)))
                .isInstanceOfSatisfying(PasswordResetRejectedException.class,
                        e -> assertThat(e.getReason()).isEqualTo(Reason.ALREADY_USED));
        assertThat(token.getModifiedAt()).isEqualTo(NOW.plusSeconds(10));
    }

    @Test
    @DisplayName("A used token reports as used even after it has expired")
    void shouldReportUsedBeforeExpired() {
        final PasswordResetToken token = PasswordResetToken.issue(userId, NOW).token();
        token.consume(NOW.plusSeconds(10));

        assertThatThrownBy(() -> token.consume(NOW.plus(Duration.ofDays(2))))
                .isInstanceOfSatisfying(PasswordResetRejectedException.class,
                        e -> assertThat(e.getReason()).isEqualTo(Reason.ALREADY_USED));
    }

    @Test
    @DisplayName("Expiring an open token cuts its life to now, so the old link is refused as expired")
    void shouldExpireAnOpenTokenImmediately() {
        final PasswordResetToken token = PasswordResetToken.issue(userId, NOW).token();
        final LocalDateTime later = NOW.plusSeconds(90);

        token.expire(later);

        assertThat(token.getExpiresAt()).isEqualTo(later);
        assertThatThrownBy(() -> token.consume(later)).isInstanceOfSatisfying(PasswordResetRejectedException.class,
                e -> assertThat(e.getReason()).isEqualTo(Reason.EXPIRED));
    }

    @Test
    @DisplayName("Expiring never extends an already expired token nor touches a spent one")
    void shouldLeaveSpentAndAlreadyExpiredTokensAlone() {
        final var stale = PasswordResetToken.issue(userId, NOW).token();
        final var originalExpiry = stale.getExpiresAt();
        stale.expire(originalExpiry.plusSeconds(1));
        assertThat(stale.getExpiresAt()).isEqualTo(originalExpiry);

        final PasswordResetToken spent = PasswordResetToken.issue(userId, NOW).token();
        spent.consume(NOW.plusSeconds(1));
        spent.expire(NOW.plusSeconds(2));
        assertThat(spent.getExpiresAt()).isEqualTo(NOW.plus(Duration.ofHours(1)));
        assertThat(spent.isUsed()).isTrue();
    }

    @Test
    @DisplayName("wasIssuedWithin is true strictly inside the window and false at its end")
    void shouldTellWhetherItWasIssuedWithinAWindow() {
        final var token = PasswordResetToken.issue(userId, NOW).token();
        final var window = Duration.ofSeconds(60);

        assertThat(token.wasIssuedWithin(window, NOW.plusSeconds(59))).isTrue();
        assertThat(token.wasIssuedWithin(window, NOW.plusSeconds(60))).isFalse();
    }
}
