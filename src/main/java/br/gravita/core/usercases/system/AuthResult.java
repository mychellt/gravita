package br.gravita.core.usercases.system;

public record AuthResult(AuthStatus status, String sessionToken) {

	public static AuthResult authenticated(final String sessionToken) {
		return new AuthResult(AuthStatus.AUTHENTICATED, sessionToken);
	}

	public static AuthResult totpRequired() {
		return new AuthResult(AuthStatus.TOTP_REQUIRED, null);
	}

	public static AuthResult rejected() {
		return new AuthResult(AuthStatus.REJECTED, null);
	}
}
