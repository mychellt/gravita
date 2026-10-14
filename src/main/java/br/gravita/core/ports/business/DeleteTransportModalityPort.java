package br.gravita.core.ports.business;

import br.gravita.core.domain.Command;

/** Deactivates a transport modality (soft delete): the row stays so documents that used it are unaffected. */
public interface DeleteTransportModalityPort extends Command<Void> {
}
