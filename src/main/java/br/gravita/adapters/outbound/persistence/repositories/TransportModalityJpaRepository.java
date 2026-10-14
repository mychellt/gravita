package br.gravita.adapters.outbound.persistence.repositories;

import br.gravita.adapters.outbound.persistence.entities.TransportModalityJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransportModalityJpaRepository extends JpaRepository<TransportModalityJpaEntity, UUID> {

	List<TransportModalityJpaEntity> findAllByOrderByNameAsc();

	List<TransportModalityJpaEntity> findAllByActiveTrueOrderByNameAsc();

	Optional<TransportModalityJpaEntity> findFirstByCodeAndActiveTrue(String code);
}
