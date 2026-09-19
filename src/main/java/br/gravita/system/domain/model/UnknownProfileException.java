package br.gravita.system.domain.model;

import br.gravita.shared.BusinessRuleException;

import java.util.UUID;

public class UnknownProfileException extends BusinessRuleException {

	public UnknownProfileException(UUID profileId) {
		super("Unknown profile: " + profileId);
	}
}
