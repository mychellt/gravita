package br.gravita.adapters.outbound.security;

import br.gravita.adapters.outbound.persistence.entities.tax.UserJpaEntity;
import br.gravita.adapters.outbound.persistence.repositories.tax.UserJpaRepository;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.ports.outbound.security.TotpVerificationPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

/**
 * RFC 6238 TOTP verification (Google Authenticator/Authy-compatible, doc §11.1) against a
 * per-user secret stored on {@code UserJpaEntity#totpSecret}. There is no enrollment use case
 * yet in M10's roadmap, so a user without a provisioned secret simply never verifies - 2FA stays
 * fail-closed rather than silently bypassed.
 */
@Component
public class TotpVerificationAdapter implements TotpVerificationPort {

	private static final int TIME_STEP_SECONDS = 30;
	private static final int CODE_DIGITS = 6;
	private static final int ALLOWED_WINDOW_DRIFT = 1;
	private static final String HMAC_ALGORITHM = "HmacSHA1";
	private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

	private final UserJpaRepository userJpaRepository;
	private final Clock clock;

	@Autowired
	public TotpVerificationAdapter(UserJpaRepository userJpaRepository) {
		this(userJpaRepository, Clock.systemUTC());
	}

	TotpVerificationAdapter(UserJpaRepository userJpaRepository, Clock clock) {
		this.userJpaRepository = userJpaRepository;
		this.clock = clock;
	}

	@Override
	public boolean verify(UserId userId, String code) {
		Optional<UserJpaEntity> entity = userJpaRepository.findById(userId.value());
		if (entity.isEmpty() || entity.get().getTotpSecret() == null || code == null) {
			return false;
		}

		byte[] secretKey = base32Decode(entity.get().getTotpSecret());
		long currentWindow = Instant.now(clock).getEpochSecond() / TIME_STEP_SECONDS;

		for (long window = currentWindow - ALLOWED_WINDOW_DRIFT; window <= currentWindow + ALLOWED_WINDOW_DRIFT; window++) {
			if (constantTimeEquals(code, generateCode(secretKey, window))) {
				return true;
			}
		}
		return false;
	}

	private static String generateCode(byte[] secretKey, long window) {
		try {
			byte[] data = ByteBuffer.allocate(8).putLong(window).array();
			Mac mac = Mac.getInstance(HMAC_ALGORITHM);
			mac.init(new SecretKeySpec(secretKey, HMAC_ALGORITHM));
			byte[] hash = mac.doFinal(data);

			int offset = hash[hash.length - 1] & 0xF;
			int binary = ((hash[offset] & 0x7f) << 24)
					| ((hash[offset + 1] & 0xff) << 16)
					| ((hash[offset + 2] & 0xff) << 8)
					| (hash[offset + 3] & 0xff);

			int otp = binary % (int) Math.pow(10, CODE_DIGITS);
			return String.format(Locale.ROOT, "%0" + CODE_DIGITS + "d", otp);
		} catch (GeneralSecurityException e) {
			throw new IllegalStateException("Failed to compute TOTP code", e);
		}
	}

	private static boolean constantTimeEquals(String a, String b) {
		return MessageDigest.isEqual(a.getBytes(StandardCharsets.US_ASCII), b.getBytes(StandardCharsets.US_ASCII));
	}

	private static byte[] base32Decode(String base32Secret) {
		String sanitized = base32Secret.trim().toUpperCase(Locale.ROOT).replace("=", "");
		byte[] result = new byte[sanitized.length() * 5 / 8];

		int buffer = 0;
		int bitsLeft = 0;
		int index = 0;
		for (char c : sanitized.toCharArray()) {
			int value = BASE32_ALPHABET.indexOf(c);
			if (value < 0) {
				continue;
			}
			buffer = (buffer << 5) | value;
			bitsLeft += 5;
			if (bitsLeft >= 8) {
				result[index++] = (byte) ((buffer >> (bitsLeft - 8)) & 0xFF);
				bitsLeft -= 8;
			}
		}
		return result;
	}
}
