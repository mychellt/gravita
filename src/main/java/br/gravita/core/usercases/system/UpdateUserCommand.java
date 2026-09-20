package br.gravita.core.usercases.system;

import br.gravita.core.domain.system.UserStatus;

import java.util.UUID;

public record UpdateUserCommand(UUID userId, String name, String email, UUID profileId, UserStatus status) {
}
