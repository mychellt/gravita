package br.gravita.system.application.port.in;

import br.gravita.system.domain.model.UserId;

public interface RegisterUserUseCase {
	UserId execute(RegisterUserCommand command);
}
