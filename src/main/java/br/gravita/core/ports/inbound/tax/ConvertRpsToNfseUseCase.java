package br.gravita.core.ports.inbound.tax;

import br.gravita.core.domain.tax.NfseId;
import java.util.List;

public interface ConvertRpsToNfseUseCase {

	/** Returns the NFSe ids in the order the RPS ids were given; converting an RPS again returns the same id. */
	List<NfseId> execute(ConvertRpsToNfseCommand command);
}
