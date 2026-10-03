package br.gravita.core.usercases.system;

public interface ActivateAccountUseCase {

	/** @throws br.gravita.core.domain.system.ActivationRejectedException when the token is unknown, expired or spent */
	void execute(String rawToken);
}
