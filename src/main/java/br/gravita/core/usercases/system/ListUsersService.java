package br.gravita.core.usercases.system;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.Context;
import br.gravita.core.domain.system.ProfileReference;
import br.gravita.core.domain.system.User;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.ports.outbound.persistence.system.ProfileRepositoryPort;
import br.gravita.core.ports.outbound.persistence.system.UserRepositoryPort;
import br.gravita.core.usercases.CallerCompanyResolver;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@UseCase
public class ListUsersService implements ListUsersUseCase {

	private final UserRepositoryPort userRepositoryPort;
	private final ProfileRepositoryPort profileRepositoryPort;
	private final CallerCompanyResolver callerCompanyResolver;

	public ListUsersService(final UserRepositoryPort userRepositoryPort, final ProfileRepositoryPort profileRepositoryPort,
			final CallerCompanyResolver callerCompanyResolver) {
		this.userRepositoryPort = userRepositoryPort;
		this.profileRepositoryPort = profileRepositoryPort;
		this.callerCompanyResolver = callerCompanyResolver;
	}

	@Override
	public List<UserSummary> execute(final UserId callerId) {
		return callerCompanyResolver.resolve(new Context().withCaller(callerId))
				.map(this::summariesOf)
				.orElse(List.of());
	}

	private List<UserSummary> summariesOf(final UUID companyId) {
		final Map<UUID, String> profileNames = profileRepositoryPort.findAll().stream()
				.collect(Collectors.toMap(ProfileReference::id, ProfileReference::name));
		return userRepositoryPort.findAllByCompanyId(companyId).stream()
				.map(user -> summaryOf(user, profileNames))
				.toList();
	}

	private static UserSummary summaryOf(final User user, final Map<UUID, String> profileNames) {
		return new UserSummary(user.getId().value(), user.getName(), user.getEmail(), user.getProfileId(),
				profileNames.get(user.getProfileId()), user.isTwoFactorEnabled(), user.getStatus());
	}
}
