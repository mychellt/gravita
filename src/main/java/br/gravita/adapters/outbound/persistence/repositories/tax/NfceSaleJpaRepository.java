package br.gravita.adapters.outbound.persistence.repositories.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.NfceSaleJpaEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NfceSaleJpaRepository extends JpaRepository<NfceSaleJpaEntity, UUID> {
}
