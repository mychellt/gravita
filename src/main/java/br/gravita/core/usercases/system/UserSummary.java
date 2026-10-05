package br.gravita.core.usercases.system;

import br.gravita.core.domain.system.UserStatus;

import java.util.UUID;

public record UserSummary(UUID id, String name, String email, UUID profileId, String profileName,
		boolean twoFactorEnabled, UserStatus status) {
}
