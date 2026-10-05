package br.gravita.core.usercases.tax;

import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.annotations.UseCase;
import br.gravita.core.usercases.system.UpdateUserCommand;
import br.gravita.core.usercases.system.UpdateUserUseCase;
import br.gravita.core.ports.outbound.persistence.system.ProfileRepositoryPort;
import br.gravita.core.ports.outbound.persistence.system.UserRepositoryPort;
import br.gravita.core.domain.system.ProfileReference;
import br.gravita.core.domain.system.UnknownProfileException;
import br.gravita.core.domain.system.User;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.domain.system.UserNotFoundException;

@UseCase
public class UpdateUserService implements UpdateUserUseCase {

	private final UserRepositoryPort userRepositoryPort;
	private final ProfileRepositoryPort profileRepositoryPort;

	public UpdateUserService(final UserRepositoryPort userRepositoryPort, final ProfileRepositoryPort profileRepositoryPort) {
		this.userRepositoryPort = userRepositoryPort;
		this.profileRepositoryPort = profileRepositoryPort;
	}

	@Override
	public void execute(final UpdateUserCommand command) {
		final User user = userRepositoryPort.findById(UserId.of(command.userId()))
				.orElseThrow(() -> new UserNotFoundException(command.userId()));

		if (command.email() != null
				&& !command.email().equalsIgnoreCase(user.getEmail())
				&& userRepositoryPort.existsByEmail(command.email())) {
			throw new BusinessRuleException("Email already registered: " + command.email());
		}

		ProfileReference profile = null;
		if (command.profileId() != null) {
			profile = profileRepositoryPort.findById(command.profileId())
					.orElseThrow(() -> new UnknownProfileException(command.profileId()));
		}

		user.update(command.name(), command.email(), profile, command.status());
		userRepositoryPort.update(user);
	}
}
