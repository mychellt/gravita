package br.gravita.core.ports.integration;

import br.gravita.core.domain.AddressDomain;
import br.gravita.core.domain.Command;

import java.util.Optional;

public interface CepLookupPort extends Command<Optional<AddressDomain>> {
}
