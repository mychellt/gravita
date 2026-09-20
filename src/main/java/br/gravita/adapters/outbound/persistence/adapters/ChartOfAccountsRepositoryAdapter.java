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
    public ChartOfAccountsDomain save(ChartOfAccountsDomain model) {
        ChartOfAccountsJpaEntity entity = mapper.map(model);
        entity.setNew(!jpaRepository.existsById(entity.getId()));
        ChartOfAccountsJpaEntity saved = jpaRepository.save(entity);
        return mapper.map(saved);
    }

    @Override
    public Optional<ChartOfAccountsDomain> get(UUID id) {
        return jpaRepository.findById(id).map(mapper::map);
    }

    @Override
    public List<ChartOfAccountsDomain> findAll() {
        return jpaRepository.findAll().stream().map(mapper::map).toList();
    }

    @Override
    public void deleteById(UUID id) {
        // Not jpaRepository.deleteById(id): Spring Data's delete() no-ops whenever
        // Persistable#isNew() is true, which a freshly application-assigned-id
        // entity still is until Hibernate's own lifecycle callbacks flip it — so
        // look the row up first and clear the flag before deleting it.
        jpaRepository.findById(id).ifPresent(entity -> {
            entity.setNew(false);
            jpaRepository.delete(entity);
        });
    }

    @Override
    public boolean existsByParentId(UUID parentId) {
        return jpaRepository.existsByParentId(parentId);
    }
}
