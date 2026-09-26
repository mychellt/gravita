package br.gravita.core.ports.inbound.tax;

import br.gravita.core.domain.tax.NfceSaleId;

public interface RegisterNfceSaleUseCase {
	NfceSaleId execute(RegisterNfceSaleCommand command);
}
