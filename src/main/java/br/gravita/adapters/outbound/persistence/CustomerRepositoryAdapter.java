package br.gravita.adapters.outbound.persistence;

import br.gravita.core.domain.CustomerDomain;
import br.gravita.core.ports.persistence.CustomerRepositoryPort;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class CustomerRepositoryAdapter implements CustomerRepositoryPort {

    @Override
    public Optional<CustomerDomain> get(UUID id) {
        return Optional.empty();
    }

    @Override
    public List<CustomerDomain> findAll() {
        return List.of();
    }

    @Override
    public CustomerDomain save(CustomerDomain model) {
        return null;
    }
}
