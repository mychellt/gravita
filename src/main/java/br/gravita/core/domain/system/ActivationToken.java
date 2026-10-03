package br.gravita.core.domain.system;

import br.gravita.core.domain.system.ActivationRejectedException.Reason;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Single-use credential e-mailed to a new signup to prove they own the address. Only the SHA-256 hash of the secret
 * is kept, so a database leak does not hand out working activation links.
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ActivationToken {

	/** Matches the validity of {@code EmailVerification}'s codes. */
	public static final Duration VALIDITY = Duration.ofHours(24);

	private static final int SECRET_BYTES = 32;
	private static final SecureRandom RANDOM = new SecureRandom();

	private UUID id;
	private UUID userId;
	private String tokenHash;
	private Instant expiresAt;
	private Instant usedAt;
	private Instant createdAt;

	public static IssuedActivationToken issue(UserId userId, Instant now) {
		byte[] secret = new byte[SECRET_BYTES];
		RANDOM.nextBytes(secret);
		String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
		ActivationToken token = ActivationToken.builder()
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
		return usedAt != null;
	}

	/** Whether this token was issued less than {@code window} before {@code now}. */
	public boolean wasIssuedWithin(Duration window, Instant now) {
		return createdAt != null && now.isBefore(createdAt.plus(window));
	}

	/** Kills a still-open token (a newer one replaces it); a spent or already expired token is left as it was. */
	public void expire(Instant now) {
		if (!isUsed() && expiresAt.isAfter(now)) {
			this.expiresAt = now;
		}
	}

	/** Marks the token spent; refuses one that was already used or has expired (a used token wins over an expired one). */
	public void consume(Instant now) {
		if (isUsed()) {
			throw new ActivationRejectedException(Reason.ALREADY_USED, "Este link de ativação já foi utilizado.");
		}
		if (!now.isBefore(expiresAt)) {
			throw new ActivationRejectedException(Reason.EXPIRED, "Este link de ativação expirou.");
		}
		this.usedAt = now;
	}
}
