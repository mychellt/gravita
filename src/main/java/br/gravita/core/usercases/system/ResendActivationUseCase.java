package br.gravita.core.usercases.system;

public interface ResendActivationUseCase {

	/**
	 * Asks for a new activation e-mail. Deliberately returns nothing: an unknown address, an already active account
	 * and a request inside the cooldown are all silent no-ops, so the caller cannot tell them from a real resend.
	 */
	void execute(String email);
}
