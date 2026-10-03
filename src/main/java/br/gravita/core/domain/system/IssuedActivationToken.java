package br.gravita.core.domain.system;

/**
 * A freshly issued activation token. {@code rawToken} is the secret that goes into the e-mailed link; it is never
 * stored - only its hash lives on {@code token}.
 */
public record IssuedActivationToken(String rawToken, ActivationToken token) {
}
