package br.gravita.core.usercases.tax;

import br.gravita.core.domain.exceptions.ForbiddenException;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.annotations.UseCase;
import br.gravita.core.usercases.CallerCompanyResolver;
import br.gravita.core.domain.Context;
import br.gravita.core.usercases.system.RegisterUserCommand;
import br.gravita.core.usercases.system.RegisterUserUseCase;
import br.gravita.core.ports.outbound.persistence.system.ProfileRepositoryPort;
import br.gravita.core.ports.outbound.persistence.system.UserRepositoryPort;
import br.gravita.core.domain.system.ProfileReference;
import br.gravita.core.domain.system.UnknownProfileException;
import br.gravita.core.domain.system.User;
import br.gravita.core.domain.system.UserId;

import java.util.UUID;

@UseCase
public class RegisterUserService implements RegisterUserUseCase {

	private final UserRepositoryPort userRepositoryPort;
	private final ProfileRepositoryPort profileRepositoryPort;
	private final CallerCompanyResolver callerCompanyResolver;

	public RegisterUserService(final UserRepositoryPort userRepositoryPort, final ProfileRepositoryPort profileRepositoryPort,
			final CallerCompanyResolver callerCompanyResolver) {
		this.userRepositoryPort = userRepositoryPort;
		this.profileRepositoryPort = profileRepositoryPort;
		this.callerCompanyResolver = callerCompanyResolver;
	}

	@Override
	public UserId execute(final RegisterUserCommand command) {
		final UUID companyId = callerCompanyResolver.resolve(new Context().withCaller(command.callerId()))
				.orElseThrow(() -> new ForbiddenException("The caller does not belong to a company"));

		if (userRepositoryPort.existsByEmail(command.email())) {
			throw new BusinessRuleException("Email already registered: " + command.email());
		}

		final ProfileReference profile = profileRepositoryPort.findById(command.profileId())
				.orElseThrow(() -> new UnknownProfileException(command.profileId()));

		final User user = User.register(command.name(), command.email(), command.rawPassword(), profile, companyId);
		return userRepositoryPort.save(user).getId();
	}
}
