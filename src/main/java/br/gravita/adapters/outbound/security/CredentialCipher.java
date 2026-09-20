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

/**
 * AES-256-GCM encryption for {@code credential_payload} at rest (doc §11.4). The IV is
 * generated per encryption and prepended to the ciphertext so a single opaque column is
 * enough to store and later decrypt the value; the key is provisioned out-of-band via
 * {@code gravita.system.integration-credential-encryption-key}, never checked into a
 * production profile.
 */
@Component
public class CredentialCipher {

	private static final String TRANSFORMATION = "AES/GCM/NoPadding";
	private static final int GCM_TAG_LENGTH_BITS = 128;
	private static final int IV_LENGTH_BYTES = 12;

	private final SecretKeySpec key;
	private final SecureRandom secureRandom = new SecureRandom();

	public CredentialCipher(@Value("${gravita.system.integration-credential-encryption-key}") String base64Key) {
		this.key = new SecretKeySpec(Base64.getDecoder().decode(base64Key), "AES");
	}

	public String encrypt(String plaintext) {
		try {
			byte[] iv = new byte[IV_LENGTH_BYTES];
			secureRandom.nextBytes(iv);

			Cipher cipher = Cipher.getInstance(TRANSFORMATION);
			cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
			byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

			return Base64.getEncoder().encodeToString(
					ByteBuffer.allocate(iv.length + ciphertext.length).put(iv).put(ciphertext).array());
		} catch (GeneralSecurityException e) {
			throw new IllegalStateException("Failed to encrypt integration credential payload", e);
		}
	}

	public String decrypt(String encoded) {
		try {
			ByteBuffer buffer = ByteBuffer.wrap(Base64.getDecoder().decode(encoded));
			byte[] iv = new byte[IV_LENGTH_BYTES];
			buffer.get(iv);
			byte[] ciphertext = new byte[buffer.remaining()];
			buffer.get(ciphertext);

			Cipher cipher = Cipher.getInstance(TRANSFORMATION);
			cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
			return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
		} catch (GeneralSecurityException e) {
			throw new IllegalStateException("Failed to decrypt integration credential payload", e);
		}
	}
}
