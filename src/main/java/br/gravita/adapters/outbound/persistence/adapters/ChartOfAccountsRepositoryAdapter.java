package br.gravita.adapters.outbound.persistence.adapters;

import br.gravita.adapters.outbound.persistence.entities.ChartOfAccountsJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.ChartOfAccountsPersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.ChartOfAccountsJpaRepository;
import br.gravita.core.domain.ChartOfAccountsDomain;
import br.gravita.core.ports.outbound.persistence.ChartOfAccountsRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Component
public class ChartOfAccountsRepositoryAdapter implements ChartOfAccountsRepositoryPort {

    private final ChartOfAccountsJpaRepository jpaRepository;
    private final ChartOfAccountsPersistenceMapper mapper;

    @Override
    public ChartOfAccountsDomain save(final ChartOfAccountsDomain model) {
        final ChartOfAccountsJpaEntity entity = mapper.map(model);
        entity.setNew(!jpaRepository.existsById(entity.getId()));
        final ChartOfAccountsJpaEntity saved = jpaRepository.save(entity);
        return mapper.map(saved);
    }

    @Override
    public Optional<ChartOfAccountsDomain> get(final UUID id) {
        return jpaRepository.findById(id).map(mapper::map);
    }

    @Override
    public List<ChartOfAccountsDomain> findAll() {
        return jpaRepository.findAll().stream().map(mapper::map).toList();
    }

    @Override
    public void deleteById(final UUID id) {
        jpaRepository.findById(id).ifPresent(entity -> {
            entity.setNew(false);
            jpaRepository.delete(entity);
        });
    }

    @Override
    public boolean existsByParentId(final UUID parentId) {
        return jpaRepository.existsByParentId(parentId);
    }
}
