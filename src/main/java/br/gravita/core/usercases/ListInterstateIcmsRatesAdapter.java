package br.gravita.core.usercases;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.InterstateIcmsRateDomain;
import br.gravita.core.ports.business.ListInterstateIcmsRatesPort;
import br.gravita.core.ports.outbound.persistence.InterstateIcmsRateRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ListInterstateIcmsRatesAdapter implements ListInterstateIcmsRatesPort {

	private final InterstateIcmsRateRepositoryPort interstateIcmsRateRepositoryPort;

	public ListInterstateIcmsRatesAdapter(final InterstateIcmsRateRepositoryPort interstateIcmsRateRepositoryPort) {
		this.interstateIcmsRateRepositoryPort = interstateIcmsRateRepositoryPort;
	}

	@Override
	public List<InterstateIcmsRateDomain> execute(final Context context) {
		return interstateIcmsRateRepositoryPort.findAll();
	}
}
