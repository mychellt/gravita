package br.gravita.core.ports.outbound.persistence;

import br.gravita.core.domain.masterdata.Supplier;
import br.gravita.core.domain.masterdata.SupplierId;
import java.util.Optional;

public interface SupplierRepositoryPort {
	Supplier save(Supplier supplier);
	Optional<Supplier> findById(SupplierId id);
}
