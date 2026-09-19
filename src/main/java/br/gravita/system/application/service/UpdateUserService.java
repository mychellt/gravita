package br.gravita.system.application.service;

import br.gravita.shared.BusinessRuleException;
import br.gravita.shared.UseCase;
import br.gravita.system.application.port.in.UpdateUserCommand;
import br.gravita.system.application.port.in.UpdateUserUseCase;
import br.gravita.system.application.port.out.ProfileRepositoryPort;
import br.gravita.system.application.port.out.UserRepositoryPort;
import br.gravita.system.domain.model.ProfileReference;
import br.gravita.system.domain.model.UnknownProfileException;
import br.gravita.system.domain.model.User;
import br.gravita.system.domain.model.UserId;
import br.gravita.system.domain.model.UserNotFoundException;

@UseCase
public class UpdateUserService implements UpdateUserUseCase {

	private final UserRepositoryPort userRepositoryPort;
	private final ProfileRepositoryPort profileRepositoryPort;

	public UpdateUserService(UserRepositoryPort userRepositoryPort, ProfileRepositoryPort profileRepositoryPort) {
		this.userRepositoryPort = userRepositoryPort;
		this.profileRepositoryPort = profileRepositoryPort;
	}

	@Override
	public void execute(UpdateUserCommand command) {
		User user = userRepositoryPort.findById(UserId.of(command.userId()))
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
