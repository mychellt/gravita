package br.gravita.adapters.outbound.security;

import br.gravita.core.ports.outbound.security.PasswordVerificationPort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Hashes credentials with bcrypt (doc §11.4) at the persistence boundary, mirroring how {@code
 * CredentialCipher} keeps encryption out of the domain layer. Unlike that cipher this is one-way
 * on purpose: a stored {@code User} never needs its plaintext password back - {@link #matches}
 * (backing {@code PasswordVerificationPort} for AuthenticateUseCase) only confirms equivalence.
 */
@Component
public class PasswordHasher implements PasswordVerificationPort {

	private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

	public String hash(String rawPassword) {
		return encoder.encode(rawPassword);
	}

	@Override
	public boolean matches(String rawPassword, String hash) {
		return encoder.matches(rawPassword, hash);
	}
}
