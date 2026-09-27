package br.gravita.core.ports.outbound.persistence.tax;

import br.gravita.core.domain.tax.InboundManifestation;
import java.util.List;

/**
 * Dedicated persistence for {@link InboundManifestation}: deliberately
 * exposes no update or delete operation, since a recorded manifestation
 * already accepted by SEFAZ must never be revised after the fact - same
 * rationale as {@code VoidedNumberRangeRepositoryPort}.
 */
public interface InboundManifestationRepositoryPort {

	InboundManifestation save(InboundManifestation manifestation);

	List<InboundManifestation> findByAccessKey(String accessKey);
}
