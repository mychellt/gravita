package br.gravita.adapters.inbound.controllers.tax.dtos;

import br.gravita.core.domain.tax.NfseId;
import java.util.List;
import java.util.UUID;

public record ConvertRpsToNfseResponse(List<UUID> ids) {

	public static ConvertRpsToNfseResponse from(List<NfseId> ids) {
		return new ConvertRpsToNfseResponse(ids.stream().map(NfseId::value).toList());
	}
}
