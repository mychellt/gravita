package br.gravita.core.ports.business;

import br.gravita.core.domain.Command;
import br.gravita.core.domain.InterstateIcmsRateDomain;

import java.util.List;

public interface ListInterstateIcmsRatesPort extends Command<List<InterstateIcmsRateDomain>> {
}
