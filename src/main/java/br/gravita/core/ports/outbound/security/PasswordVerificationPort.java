package br.gravita.core.ports.outbound.security;

public interface PasswordVerificationPort {

	boolean matches(String rawPassword, String hash);
}
