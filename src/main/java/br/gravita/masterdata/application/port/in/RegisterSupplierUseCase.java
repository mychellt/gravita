package br.gravita.masterdata.application.port.in;

import br.gravita.masterdata.domain.model.SupplierId;

public interface RegisterSupplierUseCase {
	SupplierId execute(RegisterSupplierCommand command);
}
