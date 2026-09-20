package br.gravita.core.ports.integration;

import br.gravita.core.domain.PersonLookupResult;
import br.gravita.shared.Document;

import java.util.Optional;

public interface CnpjLookupPort {
	Optional<PersonLookupResult> lookup(Document cnpj);
}
