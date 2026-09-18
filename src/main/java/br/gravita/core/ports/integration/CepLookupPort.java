package br.gravita.core.ports.integration;

import br.gravita.core.domain.AddressDomain;

import java.util.Optional;

public interface CepLookupPort {
	Optional<AddressDomain> lookup(String cep);
}
