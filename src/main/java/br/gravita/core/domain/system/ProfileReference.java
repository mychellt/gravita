package br.gravita.core.domain.system;

import java.util.UUID;

public record ProfileReference(UUID id, String name) {

	private static final String ADMINISTRATOR_PROFILE_NAME = "Administrator";
	public static final String GRAVITA_ADMINISTRATOR_PROFILE_NAME = "GRAVITA_ADMINISTRATOR";

	public boolean isAdministrator() {
		return ADMINISTRATOR_PROFILE_NAME.equalsIgnoreCase(name);
	}

	/** Gravita staff, the only profile allowed into the platform administration area. */
	public boolean isGravitaAdministrator() {
		return GRAVITA_ADMINISTRATOR_PROFILE_NAME.equals(name);
	}
}
