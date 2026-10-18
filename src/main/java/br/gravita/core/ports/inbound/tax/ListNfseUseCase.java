package br.gravita.core.ports.inbound.tax;

import br.gravita.core.domain.Command;
import java.util.List;

/**
 * Every RPS/NFSe the caller's company issued, at any status, newest first. The company is resolved server-side from
 * the authenticated caller, never taken from the client.
 */
public interface ListNfseUseCase extends Command<List<NfseListItem>> {
}
