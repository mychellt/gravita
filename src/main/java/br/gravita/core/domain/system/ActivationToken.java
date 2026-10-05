package br.gravita.core.domain.system;

import br.gravita.core.domain.AbstractDomain;
import br.gravita.core.domain.system.ActivationRejectedException.Reason;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class ActivationToken extends AbstractDomain {
    public static final Duration VALIDITY = Duration.ofHours(24);

    private UUID id;
    private UUID userId;
    private String tokenHash;
    private LocalDateTime expiresAt;

    public static IssuedActivationToken issue(final UserId userId) {
        return issue(userId, LocalDateTime.now());
    }

    public static IssuedActivationToken issue(final UserId userId, final LocalDateTime now) {
        final var rawToken = TokenSecret.generate();
        final var token = ActivationToken.builder()
                .id(UUID.randomUUID())
                .userId(userId.value())
                .tokenHash(hash(rawToken))
                .expiresAt(now.plus(VALIDITY))
                .createdAt(now)
                .build();
        return new IssuedActivationToken(rawToken, token);
    }

    public static String hash(final String rawToken) {
        return TokenSecret.hash(rawToken);
    }

    public boolean isUsed() {
        return getModifiedAt() != null;
    }

    public boolean wasIssuedWithin(final Duration window, final LocalDateTime now) {
        return getCreatedAt() != null && now.isBefore(getCreatedAt().plus(window));
    }

    public void expire(final LocalDateTime now) {
        if (!isUsed() && expiresAt.isAfter(now)) {
            this.expiresAt = now;
        }
    }

    public void consume(final LocalDateTime now) {
        if (isUsed()) {
            throw new ActivationRejectedException(Reason.ALREADY_USED, "Este link de ativação já foi utilizado.");
        }
        if (!now.isBefore(expiresAt)) {
            throw new ActivationRejectedException(Reason.EXPIRED, "Este link de ativação expirou.");
        }
        setModifiedAt(now);
    }
}
