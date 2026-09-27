package br.gravita.core.domain.shared;

import java.util.UUID;

/**
 * Reference to a person/company already registered elsewhere (e.g. a
 * `masterdata` customer), kept alongside a fiscal document's own copy of that
 * person's data (see {@code NfeRecipient}) rather than requiring a live
 * lookup at issuance time.
 */
public record PersonRef(UUID id) {

	public static PersonRef of(UUID id) {
		return id == null ? null : new PersonRef(id);
	}
}
