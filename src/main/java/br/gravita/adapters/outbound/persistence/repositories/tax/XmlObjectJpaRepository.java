package br.gravita.adapters.outbound.persistence.repositories.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.XmlObjectJpaEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface XmlObjectJpaRepository extends JpaRepository<XmlObjectJpaEntity, UUID> {
}
