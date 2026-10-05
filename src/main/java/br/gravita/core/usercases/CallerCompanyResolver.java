package br.gravita.core.usercases;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.exceptions.UnauthorizedException;
import br.gravita.core.domain.system.User;
import br.gravita.core.ports.outbound.persistence.system.UserRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** Resolves, server-side, the tenant company of the user behind a request; it is never taken from the client. */
@Component
public class CallerCompanyResolver {

	private final UserRepositoryPort userRepositoryPort;

	public CallerCompanyResolver(final UserRepositoryPort userRepositoryPort) {
		this.userRepositoryPort = userRepositoryPort;
	}

	/** Empty when the caller has no company (e.g. a user that predates the tenant link). */
	public Optional<UUID> resolve(final Context context) {
		if (context.getCaller() == null) {
			throw new UnauthorizedException("Authenticated caller is required");
		}
		return userRepositoryPort.findById(context.getCaller()).map(User::getCompanyId);
	}
}
