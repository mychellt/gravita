package br.gravita.adapters.outbound.persistence.repositories.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.NfseNumberSequenceJpaEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface NfseNumberSequenceJpaRepository extends JpaRepository<NfseNumberSequenceJpaEntity, UUID> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<NfseNumberSequenceJpaEntity> findByCompanyIdAndMunicipalityIbge(UUID companyId,
			String municipalityIbge);
}
