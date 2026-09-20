package br.gravita.core.usercases.system;

import br.gravita.core.domain.system.UserId;

public interface RegisterUserUseCase {
	UserId execute(RegisterUserCommand command);
}
