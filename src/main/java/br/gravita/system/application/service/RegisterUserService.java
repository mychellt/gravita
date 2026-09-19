package br.gravita.system.application.service;

import br.gravita.shared.BusinessRuleException;
import br.gravita.shared.UseCase;
import br.gravita.system.application.port.in.RegisterUserCommand;
import br.gravita.system.application.port.in.RegisterUserUseCase;
import br.gravita.system.application.port.out.ProfileRepositoryPort;
import br.gravita.system.application.port.out.UserRepositoryPort;
import br.gravita.system.domain.model.ProfileReference;
import br.gravita.system.domain.model.UnknownProfileException;
import br.gravita.system.domain.model.User;
import br.gravita.system.domain.model.UserId;

@UseCase
public class RegisterUserService implements RegisterUserUseCase {

	private final UserRepositoryPort userRepositoryPort;
	private final ProfileRepositoryPort profileRepositoryPort;

	public RegisterUserService(UserRepositoryPort userRepositoryPort, ProfileRepositoryPort profileRepositoryPort) {
		this.userRepositoryPort = userRepositoryPort;
		this.profileRepositoryPort = profileRepositoryPort;
	}

	@Override
	public UserId execute(RegisterUserCommand command) {
		if (userRepositoryPort.existsByEmail(command.email())) {
			throw new BusinessRuleException("Email already registered: " + command.email());
		}

		ProfileReference profile = profileRepositoryPort.findById(command.profileId())
				.orElseThrow(() -> new UnknownProfileException(command.profileId()));

		User user = User.register(command.name(), command.email(), command.rawPassword(), profile);
		return userRepositoryPort.save(user).getId();
	}
}
