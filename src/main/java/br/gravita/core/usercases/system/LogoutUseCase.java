package br.gravita.core.usercases.system;

public interface LogoutUseCase {

	/** Ends the session behind the token; a missing or unknown token is not an error. */
	void execute(String sessionToken);
}
