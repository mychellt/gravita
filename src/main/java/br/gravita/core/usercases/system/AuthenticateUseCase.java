package br.gravita.core.usercases.system;

public interface AuthenticateUseCase {
	AuthResult execute(AuthenticateCommand command);
}
