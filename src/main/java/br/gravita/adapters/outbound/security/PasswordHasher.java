package br.gravita.adapters.outbound.security;

import br.gravita.core.ports.outbound.security.PasswordVerificationPort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class PasswordHasher implements PasswordVerificationPort {

	private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

	public String hash(final String rawPassword) {
		return encoder.encode(rawPassword);
	}

	@Override
	public boolean matches(final String rawPassword, final String hash) {
		return encoder.matches(rawPassword, hash);
	}
}
