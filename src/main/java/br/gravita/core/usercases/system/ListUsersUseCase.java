package br.gravita.core.usercases.system;

import br.gravita.core.domain.system.UserId;

import java.util.List;

public interface ListUsersUseCase {

	/** The users of the company of {@code callerId}; the company is resolved server-side, never supplied by the client. */
	List<UserSummary> execute(UserId callerId);
}
