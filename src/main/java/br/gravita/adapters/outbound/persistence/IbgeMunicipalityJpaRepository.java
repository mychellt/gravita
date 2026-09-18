package br.gravita.adapters.outbound.persistence;

import br.gravita.adapters.outbound.persistence.entities.IbgeMunicipalityJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface IbgeMunicipalityJpaRepository extends JpaRepository<IbgeMunicipalityJpaEntity, UUID> {
	Optional<IbgeMunicipalityJpaEntity> findByIbgeCode(String ibgeCode);
}
