package br.gravita.system.application.port.in;

import br.gravita.system.domain.model.UserStatus;

import java.util.UUID;

public record UpdateUserCommand(UUID userId, String name, String email, UUID profileId, UserStatus status) {
}
