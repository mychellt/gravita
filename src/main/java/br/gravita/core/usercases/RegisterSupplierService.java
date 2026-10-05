package br.gravita.core.usercases;

import br.gravita.core.ports.inbound.masterdata.RegisterSupplierCommand;
import br.gravita.core.ports.inbound.masterdata.RegisterSupplierUseCase;
import br.gravita.core.ports.outbound.persistence.SupplierRepositoryPort;
import br.gravita.core.domain.masterdata.Supplier;
import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.annotations.UseCase;
import java.util.UUID;

@UseCase
public class RegisterSupplierService implements RegisterSupplierUseCase {

	private final SupplierRepositoryPort supplierRepositoryPort;

	public RegisterSupplierService(final SupplierRepositoryPort supplierRepositoryPort) {
		this.supplierRepositoryPort = supplierRepositoryPort;
	}

	@Override
	public SupplierId execute(final RegisterSupplierCommand command) {
		final SupplierId id = SupplierId.of(UUID.randomUUID());
		final Supplier supplier = Supplier.of(id, command.document(), command.name(), command.addresses(),
				command.contacts(), command.bankAccount(), command.pixKey(), command.averageLeadTimeDays(),
				command.defaultPurchaseCfop());

		final Supplier saved = supplierRepositoryPort.save(supplier);
		return saved.getId();
	}
}
