package br.gravita.core.ports.inbound.tax;

import br.gravita.core.domain.Command;
import br.gravita.core.domain.tax.InboundNfe;
import java.util.List;

/**
 * Every {@link InboundNfe} the caller's company received, at any status, newest first. The company is resolved
 * server-side from the authenticated caller, never taken from the client.
 */
public interface ListInboundNfeUseCase extends Command<List<InboundNfe>> {
}
