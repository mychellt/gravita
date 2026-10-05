package br.gravita.core.domain.system;

import br.gravita.core.domain.shared.BusinessRuleException;

import java.util.UUID;

public class UnknownProfileException extends BusinessRuleException {

	public UnknownProfileException(final UUID profileId) {
		super("Unknown profile: " + profileId);
	}
}
