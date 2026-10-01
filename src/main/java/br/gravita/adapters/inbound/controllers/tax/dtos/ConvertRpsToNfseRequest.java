package br.gravita.adapters.inbound.controllers.tax.dtos;

import br.gravita.core.domain.tax.RpsId;
import br.gravita.core.ports.inbound.tax.ConvertRpsToNfseCommand;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

/** One id converts a single RPS, several convert a batch. */
public record ConvertRpsToNfseRequest(@NotEmpty List<@NotNull UUID> rpsIds) {

	public ConvertRpsToNfseCommand toCommand() {
		return new ConvertRpsToNfseCommand(rpsIds.stream().map(RpsId::of).toList());
	}
}
