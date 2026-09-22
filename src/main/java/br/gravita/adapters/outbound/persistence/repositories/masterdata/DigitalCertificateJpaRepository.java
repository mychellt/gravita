package br.gravita.adapters.outbound.persistence.repositories.masterdata;

import br.gravita.adapters.outbound.persistence.entities.masterdata.DigitalCertificateJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DigitalCertificateJpaRepository extends JpaRepository<DigitalCertificateJpaEntity, UUID> {
	Optional<DigitalCertificateJpaEntity> findByCompanyId(UUID companyId);
}
