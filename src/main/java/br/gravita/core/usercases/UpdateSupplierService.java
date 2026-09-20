package br.gravita.core.usercases;

import br.gravita.core.domain.masterdata.Supplier;
import br.gravita.core.domain.masterdata.SupplierNotFoundException;
import br.gravita.core.annotations.UseCase;
import br.gravita.core.ports.inbound.masterdata.UpdateSupplierCommand;
import br.gravita.core.ports.inbound.masterdata.UpdateSupplierUseCase;
import br.gravita.core.ports.outbound.persistence.SupplierRepositoryPort;

/**
 * Merges the command's non-null fields onto the existing supplier and saves
 * the result; fields left {@code null} in the command keep their current
 * value (UC-M1-10). Purchase-order/return/quality history lives in
 * {@code purchasing} (M6), keyed by {@link br.gravita.core.domain.masterdata.SupplierId}
 * and never touched here, so it's unaffected by any field change.
 */
@UseCase
public class UpdateSupplierService implements UpdateSupplierUseCase {

	private final SupplierRepositoryPort supplierRepositoryPort;

	public UpdateSupplierService(SupplierRepositoryPort supplierRepositoryPort) {
		this.supplierRepositoryPort = supplierRepositoryPort;
	}

	@Override
	public void execute(UpdateSupplierCommand command) {
		Supplier existing = supplierRepositoryPort.findById(command.supplierId())
				.orElseThrow(() -> new SupplierNotFoundException(command.supplierId().value()));

		Supplier updated = Supplier.of(
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

	private static <T> T coalesce(T newValue, T currentValue) {
		return newValue != null ? newValue : currentValue;
	}
}
