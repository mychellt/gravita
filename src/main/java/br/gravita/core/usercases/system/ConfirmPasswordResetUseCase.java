package br.gravita.core.usercases.system;

public interface ConfirmPasswordResetUseCase {

	/**
	 * Sets {@code newPassword} for the owner of the reset link. Does not sign the user in.
	 *
	 * @throws br.gravita.core.domain.system.PasswordResetRejectedException when the link is unknown, expired or spent
	 */
	void execute(String rawToken, String newPassword);
}
