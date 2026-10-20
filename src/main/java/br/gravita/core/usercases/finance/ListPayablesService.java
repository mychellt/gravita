package br.gravita.core.usercases.finance;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.Context;
import br.gravita.core.domain.finance.Payable;
import br.gravita.core.domain.masterdata.Supplier;
import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.ports.inbound.finance.ListPayablesUseCase;
import br.gravita.core.ports.inbound.finance.PayableListItem;
import br.gravita.core.ports.outbound.persistence.SupplierRepositoryPort;
import br.gravita.core.ports.outbound.persistence.finance.PayableRepositoryPort;
import br.gravita.core.usercases.CallerCompanyResolver;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@UseCase
public class ListPayablesService implements ListPayablesUseCase {

	private final PayableRepositoryPort payableRepositoryPort;
	private final SupplierRepositoryPort supplierRepositoryPort;
	private final CallerCompanyResolver callerCompanyResolver;

	public ListPayablesService(final PayableRepositoryPort payableRepositoryPort, final SupplierRepositoryPort supplierRepositoryPort,
			final CallerCompanyResolver callerCompanyResolver) {
		this.payableRepositoryPort = payableRepositoryPort;
		this.supplierRepositoryPort = supplierRepositoryPort;
		this.callerCompanyResolver = callerCompanyResolver;
	}

	@Override
	public List<PayableListItem> execute(final Context context) {
		final List<Payable> payables = callerCompanyResolver.resolve(context)
				.map(payableRepositoryPort::findAllByCompany).orElse(List.of());
		final Map<UUID, String> supplierNames = new HashMap<>();
		return payables.stream().map(payable -> {
			final UUID supplierId = payable.getSupplierId();
			final String name = supplierId == null ? null
					: supplierNames.computeIfAbsent(supplierId, id -> supplierRepositoryPort.findById(SupplierId.of(id))
							.map(Supplier::getName).orElse(""));
			return new PayableListItem(payable, name == null || name.isEmpty() ? null : name);
		}).toList();
	}
}
