package br.gravita.core.ports.outbound.persistence;

import br.gravita.core.domain.masterdata.Supplier;
import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.domain.shared.Document;
import java.util.Optional;

public interface SupplierRepositoryPort {
	Supplier save(Supplier supplier);
	Optional<Supplier> findById(SupplierId id);

	/** Looks the supplier up by its CNPJ/CPF, to resolve a supplier NFe reports only as a free-text document+name. */
	Optional<Supplier> findByDocument(Document document);
}
