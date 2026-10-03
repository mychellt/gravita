package br.gravita.core.usercases.system;

import br.gravita.core.domain.system.UserId;

public record SendActivationEmailCommand(UserId userId, String name, String email) {
}
