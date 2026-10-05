package br.gravita.core.usercases.system;

import br.gravita.core.domain.Command;

/** Sends the password reset e-mail announced by {@code NotifyPasswordResetMessage}. */
public interface SendPasswordResetEmailUseCase extends Command<Void> {
}
