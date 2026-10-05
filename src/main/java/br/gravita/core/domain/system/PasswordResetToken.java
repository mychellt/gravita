package br.gravita.core.domain.system;

import br.gravita.core.domain.AbstractDomain;
import br.gravita.core.domain.system.PasswordResetRejectedException.Reason;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

/** Single-use, short-lived link that lets an active user choose a new password. Never doubles as an activation token. */
@Getter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class PasswordResetToken extends AbstractDomain {
    public static final Duration VALIDITY = Duration.ofHours(1);

    private UUID id;
    private UUID userId;
    private String tokenHash;
    private LocalDateTime expiresAt;

    public static IssuedPasswordResetToken issue(UserId userId, LocalDateTime now) {
        final var rawToken = TokenSecret.generate();
        final var token = PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .userId(userId.value())
                .tokenHash(hash(rawToken))
                .expiresAt(now.plus(VALIDITY))
                .createdAt(now)
                .build();
        return new IssuedPasswordResetToken(rawToken, token);
    }

    public static String hash(String rawToken) {
        return TokenSecret.hash(rawToken);
    }

    public boolean isUsed() {
        return getModifiedAt() != null;
    }

    public boolean wasIssuedWithin(Duration window, LocalDateTime now) {
        return getCreatedAt() != null && now.isBefore(getCreatedAt().plus(window));
    }

    /** Cuts the link short so it can no longer be used; a spent token stays as it is. */
    public void expire(LocalDateTime now) {
        if (!isUsed() && expiresAt.isAfter(now)) {
            this.expiresAt = now;
        }
    }

    public void consume(LocalDateTime now) {
        if (isUsed()) {
            throw new PasswordResetRejectedException(Reason.ALREADY_USED, "Este link de redefinição já foi utilizado.");
        }
        if (!now.isBefore(expiresAt)) {
            throw new PasswordResetRejectedException(Reason.EXPIRED, "Este link de redefinição expirou.");
        }
        setModifiedAt(now);
    }
}
