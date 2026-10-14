package br.gravita.core.ports.inbound.tax;

import br.gravita.core.domain.Command;
import br.gravita.core.domain.tax.NfeDocument;
import java.util.List;

/**
 * Every {@link NfeDocument}, at any status, issued by the caller's company, newest first. The company is resolved
 * server-side from the authenticated caller, never taken from the client.
 */
public interface ListNfeUseCase extends Command<List<NfeDocument>> {
}
