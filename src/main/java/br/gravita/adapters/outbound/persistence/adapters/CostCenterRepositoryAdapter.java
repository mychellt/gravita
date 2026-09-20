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
        CostCenterJpaEntity saved = jpaRepository.save(mapper.map(model));
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
        jpaRepository.deleteById(id);
    }

    @Override
    public boolean existsByParentId(UUID parentId) {
        return jpaRepository.existsByParentId(parentId);
    }
}
