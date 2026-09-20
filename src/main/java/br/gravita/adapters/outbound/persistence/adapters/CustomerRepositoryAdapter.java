package br.gravita.adapters.outbound.persistence.adapters;

import br.gravita.adapters.outbound.persistence.entities.CustomerJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.CustomerPersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.CustomerJpaRepository;
import br.gravita.core.domain.CustomerDomain;
import br.gravita.core.ports.outbound.persistence.CustomerRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Component
class CustomerRepositoryAdapter implements CustomerRepositoryPort {

    private final CustomerJpaRepository jpaRepository;
    private final CustomerPersistenceMapper mapper;

    @Override
    public Optional<CustomerDomain> get(UUID id) {
        return jpaRepository.findById(id).map(mapper::map);
    }

    @Override
    public List<CustomerDomain> findAll() {
        return jpaRepository.findAll().stream().map(mapper::map).toList();
    }

    @Override
    public CustomerDomain save(CustomerDomain model) {
        CustomerJpaEntity entity = mapper.map(model);
        entity.setNew(!jpaRepository.existsById(entity.getId()));
        CustomerJpaEntity saved = jpaRepository.save(entity);
        return mapper.map(saved);
    }
}
