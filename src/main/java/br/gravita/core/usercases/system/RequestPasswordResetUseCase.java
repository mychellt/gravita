package br.gravita.core.usercases.system;

public interface RequestPasswordResetUseCase {

	/**
	 * Asks for a password reset e-mail. Deliberately returns nothing: an unknown address, an account that is not
	 * active and a request inside the cooldown are all silent no-ops, so the caller cannot tell them from a real one.
	 */
	void execute(String email);
}
