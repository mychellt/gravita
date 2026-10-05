package br.gravita.adapters.outbound.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class CredentialCipher {

	private static final String TRANSFORMATION = "AES/GCM/NoPadding";
	private static final int GCM_TAG_LENGTH_BITS = 128;
	private static final int IV_LENGTH_BYTES = 12;

	private final SecretKeySpec key;
	private final SecureRandom secureRandom = new SecureRandom();

	public CredentialCipher(@Value("${gravita.system.integration-credential-encryption-key}") final String base64Key) {
		this.key = new SecretKeySpec(Base64.getDecoder().decode(base64Key), "AES");
	}

	public String encrypt(final String plaintext) {
		try {
			final byte[] iv = new byte[IV_LENGTH_BYTES];
			secureRandom.nextBytes(iv);

			final Cipher cipher = Cipher.getInstance(TRANSFORMATION);
			cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
			final byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

			return Base64.getEncoder().encodeToString(
					ByteBuffer.allocate(iv.length + ciphertext.length).put(iv).put(ciphertext).array());
		} catch (final GeneralSecurityException e) {
			throw new IllegalStateException("Failed to encrypt integration credential payload", e);
		}
	}

	public String decrypt(final String encoded) {
		try {
			final ByteBuffer buffer = ByteBuffer.wrap(Base64.getDecoder().decode(encoded));
			final byte[] iv = new byte[IV_LENGTH_BYTES];
			buffer.get(iv);
			final byte[] ciphertext = new byte[buffer.remaining()];
			buffer.get(ciphertext);

			final Cipher cipher = Cipher.getInstance(TRANSFORMATION);
			cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
			return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
		} catch (final GeneralSecurityException e) {
			throw new IllegalStateException("Failed to decrypt integration credential payload", e);
		}
	}
}
