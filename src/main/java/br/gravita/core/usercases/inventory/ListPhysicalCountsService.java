package br.gravita.core.usercases.inventory;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.inventory.DefaultWarehouse;
import br.gravita.core.domain.inventory.PhysicalCount;
import br.gravita.core.domain.inventory.PhysicalCountLine;
import br.gravita.core.domain.shared.Page;
import br.gravita.core.ports.inbound.inventory.ListPhysicalCountsQuery;
import br.gravita.core.ports.inbound.inventory.ListPhysicalCountsUseCase;
import br.gravita.core.ports.inbound.inventory.PhysicalCountItemView;
import br.gravita.core.ports.outbound.persistence.ProductRepositoryPort;
import br.gravita.core.ports.outbound.persistence.inventory.PhysicalCountRepositoryPort;
import java.util.Map;
import java.util.UUID;

@UseCase
public class ListPhysicalCountsService implements ListPhysicalCountsUseCase {

	private final PhysicalCountRepositoryPort physicalCountRepositoryPort;
	private final ProductRepositoryPort productRepositoryPort;

	public ListPhysicalCountsService(final PhysicalCountRepositoryPort physicalCountRepositoryPort,
			final ProductRepositoryPort productRepositoryPort) {
		this.physicalCountRepositoryPort = physicalCountRepositoryPort;
		this.productRepositoryPort = productRepositoryPort;
	}

	@Override
	public Page<PhysicalCountItemView> execute(final ListPhysicalCountsQuery query) {
		final Page<PhysicalCount> counts = physicalCountRepositoryPort.search(DefaultWarehouse.ID, query);
		final Map<UUID, String> codes = ProductCodeLookup.codesOf(productRepositoryPort,
				counts.content().stream().flatMap(count -> count.getLines().stream()).map(PhysicalCountLine::productId)
						.toList());
		return new Page<>(
				counts.content().stream().map(count -> new PhysicalCountItemView(count, codes)).toList(),
				counts.page(), counts.size(), counts.totalElements());
	}
}
