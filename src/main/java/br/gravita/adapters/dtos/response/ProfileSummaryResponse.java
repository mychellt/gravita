package br.gravita.adapters.dtos.response;

import br.gravita.core.domain.system.ProfileReference;

import java.util.UUID;

public record ProfileSummaryResponse(UUID id, String name) {

	public static ProfileSummaryResponse from(ProfileReference profile) {
		return new ProfileSummaryResponse(profile.id(), profile.name());
	}
}
