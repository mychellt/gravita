package br.gravita.core.ports.integration;

import br.gravita.core.domain.DocumentDomain;
import br.gravita.core.domain.PersonLookupResult;

import java.util.Optional;

public interface CnpjLookupPort {
	Optional<PersonLookupResult> lookup(DocumentDomain cnpj);
}
