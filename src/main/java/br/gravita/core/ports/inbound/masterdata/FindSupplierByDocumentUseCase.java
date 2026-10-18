package br.gravita.core.ports.inbound.masterdata;

import br.gravita.core.domain.masterdata.Supplier;
import java.util.Optional;

public interface FindSupplierByDocumentUseCase {

	/** The registered supplier with this CNPJ (any formatting accepted), or empty when none is registered. */
	Optional<Supplier> execute(String cnpj);
}
