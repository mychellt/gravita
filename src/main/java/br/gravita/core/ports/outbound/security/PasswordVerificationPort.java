package br.gravita.core.ports.outbound.security;

/**
 * Checks a plaintext password against its bcrypt hash (doc §11.4), consumed by {@code
 * AuthenticateUseCase}. {@code UserRepositoryPort} loads a persisted {@code User} with its
 * bcrypt hash sitting in {@code rawPassword} - see {@code UserPersistenceMapper#toDomain}.
 */
public interface PasswordVerificationPort {

	boolean matches(String rawPassword, String hash);
}
