package br.gravita.core.ports.inbound.masterdata;

import br.gravita.core.domain.masterdata.SupplierId;

public interface RegisterSupplierUseCase {
	SupplierId execute(RegisterSupplierCommand command);
}
