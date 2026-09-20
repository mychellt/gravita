package br.gravita.core.domain.masterdata;

import br.gravita.core.domain.shared.BusinessRuleException;
import java.util.Objects;

public record Contact(ContactType type, String value) {

	public Contact {
		Objects.requireNonNull(type, "Contact type is required");
		if (value == null || value.isBlank()) {
			throw new BusinessRuleException("Contact value is required");
		}
	}
}
