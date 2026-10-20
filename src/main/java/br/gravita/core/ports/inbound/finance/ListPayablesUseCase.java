package br.gravita.core.ports.inbound.finance;

import br.gravita.core.domain.Command;
import java.util.List;

/**
 * Every payable in the caller's company's books, at any status, latest due date first. The company is resolved
 * server-side from the authenticated caller, never taken from the client.
 */
public interface ListPayablesUseCase extends Command<List<PayableListItem>> {
}
