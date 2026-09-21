package br.gravita.core.usercases.system;

public enum AuthStatus {
	AUTHENTICATED,
	TOTP_REQUIRED,
	REJECTED
}
