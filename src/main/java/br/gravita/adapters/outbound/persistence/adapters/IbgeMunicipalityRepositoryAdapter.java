package br.gravita.adapters.outbound.persistence.adapters;

import br.gravita.adapters.outbound.persistence.mappers.IbgeMunicipalityPersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.IbgeMunicipalityJpaRepository;
import br.gravita.core.domain.IbgeMunicipalityDomain;
import br.gravita.core.ports.outbound.persistence.IbgeMunicipalityRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Repository
class IbgeMunicipalityRepositoryAdapter implements IbgeMunicipalityRepositoryPort {

    private final IbgeMunicipalityJpaRepository jpaRepository;
    private final IbgeMunicipalityPersistenceMapper mapper;

    @Override
    public Optional<IbgeMunicipalityDomain> get(UUID id) {
        return jpaRepository.findById(id).map(mapper::map);
    }

    @Override
    public List<IbgeMunicipalityDomain> findAll() {
        return jpaRepository.findAll().stream().map(mapper::map).toList();
    }

    @Override
    public Optional<IbgeMunicipalityDomain> findByIbgeCode(String ibgeCode) {
        return jpaRepository.findByIbgeCode(ibgeCode).map(mapper::map);
    }

    @Override
    public List<IbgeMunicipalityDomain> saveAll(List<IbgeMunicipalityDomain> municipalities) {
        return jpaRepository.saveAll(municipalities.stream().map(mapper::map).toList())
                .stream().map(mapper::map).toList();
    }
}
