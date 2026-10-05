package br.gravita.core.usercases.system;

import java.util.Optional;

public interface GetCurrentUserUseCase {

	/** @return the user behind the session, or empty when the token is missing, unknown or its user no longer exists */
	Optional<CurrentUser> execute(String sessionToken);
}
