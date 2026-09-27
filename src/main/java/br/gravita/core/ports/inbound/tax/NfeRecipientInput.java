package br.gravita.core.ports.inbound.tax;

import br.gravita.core.domain.IeIndicator;
import br.gravita.core.domain.shared.PersonType;
import java.util.UUID;

/**
 * The NFe recipient (module spec §3.1): either an existing {@code customerId}
 * (a registered {@code PersonRef}) or ad-hoc PF/PJ data for a walk-in
 * recipient - {@code document}/{@code documentType} and {@code name} are
 * always given directly either way, so the use case never has to branch
 * on which case it is when validating them (AC3).
 */
public record NfeRecipientInput(
		UUID customerId,
		String document,
		PersonType documentType,
		String name,
		IeIndicator ieIndicator,
		String ie,
		String state) {
}
