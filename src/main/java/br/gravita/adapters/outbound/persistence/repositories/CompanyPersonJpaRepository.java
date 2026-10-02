package br.gravita.adapters.outbound.persistence.repositories;

import br.gravita.adapters.outbound.persistence.entities.CompanyPersonJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CompanyPersonJpaRepository extends JpaRepository<CompanyPersonJpaEntity, UUID> {
	boolean existsByDocument(String document);
}
