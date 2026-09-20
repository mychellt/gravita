package br.gravita.adapters.outbound.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Hashes credentials with bcrypt (doc §11.4) at the persistence boundary, mirroring how {@code
 * CredentialCipher} keeps encryption out of the domain layer. Unlike that cipher this is one-way
 * on purpose: a stored {@code User} never needs its plaintext password back.
 */
@Component
class PasswordHasher {

	private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

	String hash(String rawPassword) {
		return encoder.encode(rawPassword);
	}
}
