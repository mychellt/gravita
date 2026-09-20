package br.gravita.adapters.outbound.persistence.adapters;

import br.gravita.adapters.outbound.persistence.entities.CostCenterJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.CostCenterPersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.CostCenterJpaRepository;
import br.gravita.core.domain.CostCenterDomain;
import br.gravita.core.ports.outbound.persistence.CostCenterRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Component
class CostCenterRepositoryAdapter implements CostCenterRepositoryPort {

    private final CostCenterJpaRepository jpaRepository;
    private final CostCenterPersistenceMapper mapper;

    @Override
    public CostCenterDomain save(CostCenterDomain model) {
        CostCenterJpaEntity entity = mapper.map(model);
        entity.setNew(!jpaRepository.existsById(entity.getId()));
        CostCenterJpaEntity saved = jpaRepository.save(entity);
        return mapper.map(saved);
    }

    @Override
    public Optional<CostCenterDomain> get(UUID id) {
        return jpaRepository.findById(id).map(mapper::map);
    }

    @Override
    public List<CostCenterDomain> findAll() {
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
