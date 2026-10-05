package br.gravita.core.domain.system;

/**
 * A freshly issued password reset token. {@code rawToken} is the secret that goes into the e-mailed link; it is
 * never stored - only its hash lives on {@code token}.
 */
public record IssuedPasswordResetToken(String rawToken, PasswordResetToken token) {
}
