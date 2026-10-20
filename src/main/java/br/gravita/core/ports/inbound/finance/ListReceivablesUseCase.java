package br.gravita.core.ports.inbound.finance;

import br.gravita.core.domain.Command;
import java.util.List;

/**
 * Every receivable of the caller's company's customers, at any status, latest due date first. The company is resolved
 * server-side from the authenticated caller, never taken from the client.
 */
public interface ListReceivablesUseCase extends Command<List<ReceivableListItem>> {
}
