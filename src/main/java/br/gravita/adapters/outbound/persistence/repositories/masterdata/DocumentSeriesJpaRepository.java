package br.gravita.adapters.outbound.persistence.repositories.masterdata;

import br.gravita.adapters.outbound.persistence.entities.masterdata.DocumentSeriesJpaEntity;
import br.gravita.core.domain.masterdata.FiscalDocumentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DocumentSeriesJpaRepository extends JpaRepository<DocumentSeriesJpaEntity, UUID> {
	Optional<DocumentSeriesJpaEntity> findByCompanyIdAndDocumentType(UUID companyId, FiscalDocumentType documentType);
}
