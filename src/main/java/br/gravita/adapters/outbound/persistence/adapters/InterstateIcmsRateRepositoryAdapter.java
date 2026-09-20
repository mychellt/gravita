package br.gravita.adapters.outbound.persistence.adapters;

import br.gravita.adapters.outbound.persistence.mappers.InterstateIcmsRatePersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.InterstateIcmsRateJpaRepository;
import br.gravita.core.domain.InterstateIcmsRateDomain;
import br.gravita.core.ports.outbound.persistence.InterstateIcmsRateRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Repository
public class InterstateIcmsRateRepositoryAdapter implements InterstateIcmsRateRepositoryPort {

    private final InterstateIcmsRateJpaRepository jpaRepository;
    private final InterstateIcmsRatePersistenceMapper mapper;

    @Override
    public Optional<InterstateIcmsRateDomain> get(UUID id) {
        return jpaRepository.findById(id).map(mapper::map);
    }

    @Override
    public List<InterstateIcmsRateDomain> findAll() {
        return jpaRepository.findAll().stream().map(mapper::map).toList();
    }

    @Override
    public Optional<InterstateIcmsRateDomain> findByOriginStateAndDestinationState(String originState, String destinationState) {
        return jpaRepository.findByOriginStateAndDestinationState(originState, destinationState).map(mapper::map);
    }

    @Override
    public List<InterstateIcmsRateDomain> saveAll(List<InterstateIcmsRateDomain> rates) {
        return jpaRepository.saveAll(rates.stream().map(mapper::map).toList())
                .stream().map(mapper::map).toList();
    }
}
