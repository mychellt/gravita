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
public class CertificateCipher {

	private static final String TRANSFORMATION = "AES/GCM/NoPadding";
	private static final int GCM_TAG_LENGTH_BITS = 128;
	private static final int IV_LENGTH_BYTES = 12;

	private final SecretKeySpec key;
	private final SecureRandom secureRandom = new SecureRandom();

	public CertificateCipher(@Value("${gravita.masterdata.certificate-encryption-key}") final String base64Key) {
		this.key = new SecretKeySpec(Base64.getDecoder().decode(base64Key), "AES");
	}

	public byte[] encrypt(final byte[] plaintext) {
		try {
			final byte[] iv = new byte[IV_LENGTH_BYTES];
			secureRandom.nextBytes(iv);

			final Cipher cipher = Cipher.getInstance(TRANSFORMATION);
			cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
			final byte[] ciphertext = cipher.doFinal(plaintext);

			return ByteBuffer.allocate(iv.length + ciphertext.length).put(iv).put(ciphertext).array();
		} catch (final GeneralSecurityException e) {
			throw new IllegalStateException("Failed to encrypt certificate payload", e);
		}
	}

	public byte[] decrypt(final byte[] encoded) {
		try {
			final ByteBuffer buffer = ByteBuffer.wrap(encoded);
			final byte[] iv = new byte[IV_LENGTH_BYTES];
			buffer.get(iv);
			final byte[] ciphertext = new byte[buffer.remaining()];
			buffer.get(ciphertext);

			final Cipher cipher = Cipher.getInstance(TRANSFORMATION);
			cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
			return cipher.doFinal(ciphertext);
		} catch (final GeneralSecurityException e) {
			throw new IllegalStateException("Failed to decrypt certificate payload", e);
		}
	}

	public String encryptText(final String plaintext) {
		return Base64.getEncoder().encodeToString(encrypt(plaintext.getBytes(StandardCharsets.UTF_8)));
	}

	public String decryptText(final String encoded) {
		return new String(decrypt(Base64.getDecoder().decode(encoded)), StandardCharsets.UTF_8);
	}
}
