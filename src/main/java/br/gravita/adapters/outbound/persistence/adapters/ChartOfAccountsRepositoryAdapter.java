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
        ChartOfAccountsJpaEntity saved = jpaRepository.save(mapper.map(model));
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
        jpaRepository.deleteById(id);
    }

    @Override
    public boolean existsByParentId(UUID parentId) {
        return jpaRepository.existsByParentId(parentId);
    }
}
