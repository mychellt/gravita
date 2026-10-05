package br.gravita.core.usercases.system;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.system.ProfileReference;
import br.gravita.core.ports.outbound.persistence.system.ProfileRepositoryPort;
import br.gravita.core.ports.outbound.persistence.system.UserRepositoryPort;
import br.gravita.core.ports.outbound.security.SessionStorePort;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@UseCase
@RequiredArgsConstructor
public class GetCurrentUserService implements GetCurrentUserUseCase {

	private final SessionStorePort sessionStorePort;
	private final UserRepositoryPort userRepositoryPort;
	private final ProfileRepositoryPort profileRepositoryPort;

	@Override
	public Optional<CurrentUser> execute(final String sessionToken) {
		if (sessionToken == null || sessionToken.isBlank()) {
			return Optional.empty();
		}
		return sessionStorePort.resolve(sessionToken)
				.flatMap(userRepositoryPort::findById)
				.map(user -> new CurrentUser(user.getName(), user.getEmail(),
						profileRepositoryPort.findById(user.getProfileId()).map(ProfileReference::name).orElse(null),
						user.getCompanyId()));
	}
}
