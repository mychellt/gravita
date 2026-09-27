package br.gravita.core.domain.system;

import java.util.UUID;

public record ProfileReference(UUID id, String name) {

	private static final String ADMINISTRATOR_PROFILE_NAME = "Administrator";

	public boolean isAdministrator() {
		return ADMINISTRATOR_PROFILE_NAME.equalsIgnoreCase(name);
	}
}
