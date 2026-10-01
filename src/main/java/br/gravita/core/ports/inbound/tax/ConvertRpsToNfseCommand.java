package br.gravita.core.ports.inbound.tax;

import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.tax.RpsId;
import java.util.List;

/** One RPS id converts a single RPS; several convert them as a batch. */
public record ConvertRpsToNfseCommand(List<RpsId> rpsIds) {

	public ConvertRpsToNfseCommand {
		if (rpsIds == null || rpsIds.isEmpty()) {
			throw new BusinessRuleException("At least one RPS id is required");
		}
		rpsIds = List.copyOf(rpsIds);
	}
}
