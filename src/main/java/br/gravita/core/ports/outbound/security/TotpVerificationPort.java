package br.gravita.core.ports.outbound.security;

import br.gravita.core.domain.system.UserId;

/**
 * Google Authenticator/Authy-compatible TOTP check (doc §11.1), consumed by {@code
 * AuthenticateUseCase} when the effective user has {@code twoFactorEnabled}.
 */
public interface TotpVerificationPort {

	boolean verify(UserId userId, String code);
}
