package br.gravita.core.ports.business;

import br.gravita.core.domain.Command;
import br.gravita.core.domain.PersonLookupResult;

public interface LookupPersonByDocumentPort extends Command<PersonLookupResult> {
}
