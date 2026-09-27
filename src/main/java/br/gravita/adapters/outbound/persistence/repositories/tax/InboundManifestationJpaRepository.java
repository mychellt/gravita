package br.gravita.adapters.outbound.persistence.repositories.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.InboundManifestationJpaEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InboundManifestationJpaRepository extends JpaRepository<InboundManifestationJpaEntity, UUID> {

	List<InboundManifestationJpaEntity> findByAccessKey(String accessKey);
}
