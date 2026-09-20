package br.gravita.core.ports.integration;

import br.gravita.core.domain.Command;
import br.gravita.core.domain.PersonLookupResult;

import java.util.Optional;

public interface CnpjLookupPort extends Command<Optional<PersonLookupResult>> {
}
