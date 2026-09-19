package br.gravita.system.domain.model;

import java.util.UUID;

/**
 * The minimal projection of a {@code Profile} (owned by the legacy profile persistence,
 * `profiles` table) that {@code RegisterUserUseCase} needs: whether it exists, and whether it's
 * the Administrator profile, since 2FA is mandatory for admin accounts (doc §11.1).
 */
public record ProfileReference(UUID id, String name) {

	private static final String ADMINISTRATOR_PROFILE_NAME = "Administrator";

	public boolean isAdministrator() {
		return ADMINISTRATOR_PROFILE_NAME.equalsIgnoreCase(name);
	}
}
