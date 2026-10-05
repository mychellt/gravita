package br.gravita.adapters.outbound.persistence.adapters;

import br.gravita.adapters.outbound.persistence.entities.CustomerJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.CustomerPersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.CustomerJpaRepository;
import br.gravita.core.domain.CustomerDomain;
import br.gravita.core.ports.outbound.persistence.CustomerRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The read methods run in a transaction because the customer's addresses, contacts and price tables are lazy
 * collections: they must be loaded while the session is open, which is when the mapper copies them to the domain.
 */
@RequiredArgsConstructor
@Component
class CustomerRepositoryAdapter implements CustomerRepositoryPort {

    private final CustomerJpaRepository jpaRepository;
    private final CustomerPersistenceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<CustomerDomain> get(final UUID id) {
        return jpaRepository.findById(id).map(mapper::map);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerDomain> findAll() {
        return jpaRepository.findAll().stream().map(mapper::map).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerDomain> findAllByCompanyId(final UUID companyId) {
        return jpaRepository.findAllByCompanyId(companyId).stream().map(mapper::map).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CustomerDomain> findByIdAndCompanyId(final UUID id, final UUID companyId) {
        return jpaRepository.findByIdAndCompanyId(id, companyId).map(mapper::map);
    }

    @Override
    public CustomerDomain save(final CustomerDomain model) {
        final CustomerJpaEntity entity = mapper.map(model);
        entity.setNew(!jpaRepository.existsById(entity.getId()));
        final CustomerJpaEntity saved = jpaRepository.save(entity);
        return mapper.map(saved);
    }
}
