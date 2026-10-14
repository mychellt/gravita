package br.gravita.core.usercases.tax;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.Context;
import br.gravita.core.domain.tax.CfopRegistryEntry;
import br.gravita.core.ports.inbound.tax.ListCfopRegistryUseCase;
import br.gravita.core.ports.outbound.tax.CfopRegistryPort;
import java.util.List;

@UseCase
public class ListCfopRegistryService implements ListCfopRegistryUseCase {

	private final CfopRegistryPort cfopRegistryPort;

	public ListCfopRegistryService(final CfopRegistryPort cfopRegistryPort) {
		this.cfopRegistryPort = cfopRegistryPort;
	}

	@Override
	public List<CfopRegistryEntry> execute(final Context context) {
		return cfopRegistryPort.findAll();
	}
}
