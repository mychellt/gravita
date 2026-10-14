package br.gravita.core.ports.inbound.tax;

import br.gravita.core.domain.Command;
import br.gravita.core.domain.tax.CfopRegistryEntry;
import java.util.List;

/** The CFOP registry (natureza da operação -> default CFOP), for the NF-e form's "Natureza da operação" select. */
public interface ListCfopRegistryUseCase extends Command<List<CfopRegistryEntry>> {
}
