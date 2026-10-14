package br.gravita.core.ports.inbound.tax;

import br.gravita.core.domain.Command;
import br.gravita.core.domain.tax.CfopRegistryEntry;

import java.util.List;

public interface ListCfopRegistryUseCase extends Command<List<CfopRegistryEntry>> {
}
