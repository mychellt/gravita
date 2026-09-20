package br.gravita.core.usercases;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.InterstateIcmsRateDomain;
import br.gravita.core.ports.business.ImportInterstateIcmsRatesPort;
import br.gravita.core.ports.outbound.persistence.InterstateIcmsRateRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/** Bulk import is the only write path for this table (AC: updated exclusively via import, no per-row edit form). */
@Component
public class ImportInterstateIcmsRatesAdapter implements ImportInterstateIcmsRatesPort {

	private final InterstateIcmsRateRepositoryPort interstateIcmsRateRepositoryPort;

	public ImportInterstateIcmsRatesAdapter(InterstateIcmsRateRepositoryPort interstateIcmsRateRepositoryPort) {
		this.interstateIcmsRateRepositoryPort = interstateIcmsRateRepositoryPort;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<InterstateIcmsRateDomain> execute(Context context) {
		List<InterstateIcmsRateDomain> incoming = (List<InterstateIcmsRateDomain>) context.getData(List.class);
		incoming.forEach(rate -> interstateIcmsRateRepositoryPort
				.findByOriginStateAndDestinationState(rate.getOriginState(), rate.getDestinationState())
				.ifPresentOrElse(
						existing -> rate.setId(existing.getId()),
						() -> rate.setId(UUID.randomUUID())));
		return interstateIcmsRateRepositoryPort.saveAll(incoming);
	}
}
