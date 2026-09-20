package br.gravita.masterdata.application.service;

import br.gravita.masterdata.application.port.in.RegisterSupplierCommand;
import br.gravita.masterdata.application.port.in.RegisterSupplierUseCase;
import br.gravita.masterdata.application.port.out.SupplierRepositoryPort;
import br.gravita.masterdata.domain.model.Supplier;
import br.gravita.masterdata.domain.model.SupplierId;
import br.gravita.shared.UseCase;
import java.util.UUID;

@UseCase
public class RegisterSupplierService implements RegisterSupplierUseCase {

	private final SupplierRepositoryPort supplierRepositoryPort;

	public RegisterSupplierService(SupplierRepositoryPort supplierRepositoryPort) {
		this.supplierRepositoryPort = supplierRepositoryPort;
	}

	@Override
	public SupplierId execute(RegisterSupplierCommand command) {
		SupplierId id = SupplierId.of(UUID.randomUUID());
		Supplier supplier = Supplier.of(id, command.document(), command.name(), command.addresses(),
				command.contacts(), command.bankAccount(), command.pixKey(), command.averageLeadTimeDays(),
				command.defaultPurchaseCfop());

		Supplier saved = supplierRepositoryPort.save(supplier);
		return saved.getId();
	}
}
