package br.gravita.adapters.outbound.persistence.repositories;

import br.gravita.adapters.outbound.persistence.entities.InterstateIcmsRateJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface InterstateIcmsRateJpaRepository extends JpaRepository<InterstateIcmsRateJpaEntity, UUID> {
	Optional<InterstateIcmsRateJpaEntity> findByOriginStateAndDestinationState(String originState, String destinationState);
}
