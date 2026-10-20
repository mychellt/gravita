package br.gravita.core.ports.inbound.tax;

import br.gravita.core.domain.Command;
import br.gravita.core.domain.tax.PosSessionId;

/**
 * The open cash-register session of the caller's company, opened (R$ 0,00 in the drawer, the caller as operator) when
 * there is none yet. The company is resolved server-side from the authenticated caller, never taken from the client.
 */
public interface GetOrOpenPosSessionUseCase extends Command<PosSessionId> {
}
