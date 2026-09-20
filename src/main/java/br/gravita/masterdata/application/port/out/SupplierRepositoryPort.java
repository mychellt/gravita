package br.gravita.masterdata.application.port.out;

import br.gravita.masterdata.domain.model.Supplier;
import br.gravita.masterdata.domain.model.SupplierId;
import java.util.Optional;

public interface SupplierRepositoryPort {
	Supplier save(Supplier supplier);
	Optional<Supplier> findById(SupplierId id);
}
