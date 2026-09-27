package br.gravita.core.ports.outbound.persistence.tax;

import br.gravita.core.domain.tax.NfceSale;
import br.gravita.core.domain.tax.NfceSaleId;
import br.gravita.core.domain.tax.PosSessionId;
import java.util.List;
import java.util.Optional;

public interface NfceRepositoryPort {

	NfceSale save(NfceSale sale);

	Optional<NfceSale> findById(NfceSaleId id);

	/**
	 * The most recently created sale system-wide, used by UC-M3-07 (AC2) to
	 * decide whether a given sale is eligible as "the last sale".
	 */
	Optional<NfceSale> findMostRecent();

	List<NfceSale> findBySessionId(PosSessionId sessionId);
}
