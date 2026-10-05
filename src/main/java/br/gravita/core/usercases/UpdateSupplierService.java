package br.gravita.core.usercases;

import br.gravita.core.domain.masterdata.Supplier;
import br.gravita.core.domain.masterdata.SupplierNotFoundException;
import br.gravita.core.annotations.UseCase;
import br.gravita.core.ports.inbound.masterdata.UpdateSupplierCommand;
import br.gravita.core.ports.inbound.masterdata.UpdateSupplierUseCase;
import br.gravita.core.ports.outbound.persistence.SupplierRepositoryPort;

@UseCase
public class UpdateSupplierService implements UpdateSupplierUseCase {

	private final SupplierRepositoryPort supplierRepositoryPort;

	public UpdateSupplierService(final SupplierRepositoryPort supplierRepositoryPort) {
		this.supplierRepositoryPort = supplierRepositoryPort;
	}

	@Override
	public void execute(final UpdateSupplierCommand command) {
		final Supplier existing = supplierRepositoryPort.findById(command.supplierId())
				.orElseThrow(() -> new SupplierNotFoundException(command.supplierId().value()));

		final Supplier updated = Supplier.of(
				existing.getId(),
				coalesce(command.document(), existing.getDocument()),
				coalesce(command.name(), existing.getName()),
				coalesce(command.addresses(), existing.getAddresses()),
				coalesce(command.contacts(), existing.getContacts()),
				coalesce(command.bankAccount(), existing.getBankAccount()),
				coalesce(command.pixKey(), existing.getPixKey()),
				coalesce(command.averageLeadTimeDays(), existing.getAverageLeadTimeDays()),
				coalesce(command.defaultPurchaseCfop(), existing.getDefaultPurchaseCfop()));

		supplierRepositoryPort.save(updated);
	}

	private static <T> T coalesce(final T newValue, final T currentValue) {
		return newValue != null ? newValue : currentValue;
	}
}
