package br.gravita.core.usercases.tax;

import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.domain.shared.UseCase;
import br.gravita.core.usercases.system.RegisterUserCommand;
import br.gravita.core.usercases.system.RegisterUserUseCase;
import br.gravita.core.ports.outbound.persistence.system.ProfileRepositoryPort;
import br.gravita.core.ports.outbound.persistence.system.UserRepositoryPort;
import br.gravita.core.domain.system.ProfileReference;
import br.gravita.core.domain.system.UnknownProfileException;
import br.gravita.core.domain.system.User;
import br.gravita.core.domain.system.UserId;

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
