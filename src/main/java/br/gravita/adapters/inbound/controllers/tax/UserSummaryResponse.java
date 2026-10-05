package br.gravita.adapters.inbound.controllers.tax;

import br.gravita.core.domain.system.UserStatus;
import br.gravita.core.usercases.system.UserSummary;

import java.util.UUID;

public record UserSummaryResponse(UUID id, String name, String email, UUID profileId, String profileName,
		boolean twoFactorEnabled, UserStatus status) {

	public static UserSummaryResponse from(final UserSummary summary) {
		return new UserSummaryResponse(summary.id(), summary.name(), summary.email(), summary.profileId(),
				summary.profileName(), summary.twoFactorEnabled(), summary.status());
	}
}
