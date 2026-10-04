package br.gravita.core.usercases.system;

import br.gravita.core.domain.Command;

/** Sends the account activation e-mail for a registration announced by {@code NotifyUserRegistrationMessage}. */
public interface SendUserRegistrationEmailUseCase extends Command<Void> {
}
