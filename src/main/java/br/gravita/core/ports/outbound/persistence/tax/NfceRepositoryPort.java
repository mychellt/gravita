package br.gravita.core.ports.outbound.persistence.tax;

import br.gravita.core.domain.tax.NfceSale;
import br.gravita.core.domain.tax.NfceSaleId;
import java.util.Optional;

public interface NfceRepositoryPort {

	NfceSale save(NfceSale sale);

	Optional<NfceSale> findById(NfceSaleId id);
}
