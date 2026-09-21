package br.gravita.core.usercases.tax;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.PermissionDomain;
import br.gravita.core.domain.ProfileDomain;
import br.gravita.core.domain.system.User;
import br.gravita.core.domain.system.UserStatus;
import br.gravita.core.ports.outbound.persistence.ProfileRepositoryPort;
import br.gravita.core.ports.outbound.persistence.system.UserRepositoryPort;
import br.gravita.core.usercases.system.CheckPermissionQuery;
import br.gravita.core.usercases.system.CheckPermissionUseCase;

import java.util.List;
import java.util.Optional;

/**
 * Both {@code UserRepositoryPort#findById} and {@code ProfileRepositoryPort#findById} hit the
 * database on every call (no cache layer), so a permission edit made by AssignProfileUseCase or
 * SaveCustomProfileUseCase - standard and custom profiles are both persisted through the same
 * {@link ProfileRepositoryPort} - is visible on the very next check.
 */
@UseCase
public class CheckPermissionService implements CheckPermissionUseCase {

	private final UserRepositoryPort userRepositoryPort;
	private final ProfileRepositoryPort profileRepositoryPort;

	public CheckPermissionService(UserRepositoryPort userRepositoryPort, ProfileRepositoryPort profileRepositoryPort) {
		this.userRepositoryPort = userRepositoryPort;
		this.profileRepositoryPort = profileRepositoryPort;
	}

	@Override
	public boolean execute(CheckPermissionQuery query) {
		Optional<User> maybeUser = userRepositoryPort.findById(query.userId());
		if (maybeUser.isEmpty() || maybeUser.get().getStatus() != UserStatus.ACTIVE) {
			return false;
		}

		Optional<ProfileDomain> maybeProfile = profileRepositoryPort.findById(maybeUser.get().getProfileId());
		if (maybeProfile.isEmpty()) {
			return false;
		}

		List<PermissionDomain> permissions = maybeProfile.get().getPermissions();
		if (permissions == null) {
			return false;
		}

		return permissions.stream().anyMatch(permission -> query.module().equals(permission.getModule())
				&& query.screen().equals(permission.getScreen())
				&& query.action() == permission.getAction());
	}
}
