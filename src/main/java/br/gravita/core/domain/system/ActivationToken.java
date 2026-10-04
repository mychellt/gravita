package br.gravita.core.domain.system;

import br.gravita.core.domain.AbstractDomain;
import br.gravita.core.domain.system.ActivationRejectedException.Reason;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Getter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class ActivationToken extends AbstractDomain {
    public static final Duration VALIDITY = Duration.ofHours(24);

    private static final int SECRET_BYTES = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    private UUID id;
    private UUID userId;
    private String tokenHash;
    private LocalDateTime expiresAt;

    public static IssuedActivationToken issue(UserId userId) {
        return issue(userId, LocalDateTime.now());
    }

    public static IssuedActivationToken issue(UserId userId, LocalDateTime now) {
        byte[] secret = new byte[SECRET_BYTES];
        RANDOM.nextBytes(secret);
        final var rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
        final var token = ActivationToken.builder()
                .id(UUID.randomUUID())
                .userId(userId.value())
                .tokenHash(hash(rawToken))
                .expiresAt(now.plus(VALIDITY))
                .createdAt(now)
                .build();
        return new IssuedActivationToken(rawToken, token);
    }

    public static String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required by every Java platform", e);
        }
    }

    public boolean isUsed() {
        return getModifiedAt() != null;
    }

    public boolean wasIssuedWithin(Duration window, LocalDateTime now) {
        return getCreatedAt() != null && now.isBefore(getCreatedAt().plus(window));
    }

    public void expire(LocalDateTime now) {
        if (!isUsed() && expiresAt.isAfter(now)) {
            this.expiresAt = now;
        }
    }

    public void consume(LocalDateTime now) {
        if (isUsed()) {
            throw new ActivationRejectedException(Reason.ALREADY_USED, "Este link de ativação já foi utilizado.");
        }
        if (!now.isBefore(expiresAt)) {
            throw new ActivationRejectedException(Reason.EXPIRED, "Este link de ativação expirou.");
        }
        setModifiedAt(now);
    }
}
