package br.gravita.core.usercases.inventory;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.ProductDomain;
import br.gravita.core.domain.inventory.PhysicalCount;
import br.gravita.core.domain.inventory.PhysicalCountId;
import br.gravita.core.domain.inventory.PhysicalCountLine;
import br.gravita.core.domain.inventory.PhysicalCountScope;
import br.gravita.core.domain.inventory.StockBalance;
import br.gravita.core.ports.inbound.inventory.StartPhysicalCountCommand;
import br.gravita.core.ports.inbound.inventory.StartPhysicalCountUseCase;
import br.gravita.core.ports.outbound.persistence.ProductRepositoryPort;
import br.gravita.core.ports.outbound.persistence.inventory.PhysicalCountRepositoryPort;
import br.gravita.core.ports.outbound.persistence.inventory.StockBalanceRepositoryPort;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@UseCase
public class StartPhysicalCountService implements StartPhysicalCountUseCase {

	private final StockBalanceRepositoryPort stockBalanceRepositoryPort;
	private final ProductRepositoryPort productRepositoryPort;
	private final PhysicalCountRepositoryPort physicalCountRepositoryPort;

	public StartPhysicalCountService(StockBalanceRepositoryPort stockBalanceRepositoryPort,
			ProductRepositoryPort productRepositoryPort, PhysicalCountRepositoryPort physicalCountRepositoryPort) {
		this.stockBalanceRepositoryPort = stockBalanceRepositoryPort;
		this.productRepositoryPort = productRepositoryPort;
		this.physicalCountRepositoryPort = physicalCountRepositoryPort;
	}

	@Override
	public PhysicalCount execute(StartPhysicalCountCommand command) {
		List<PhysicalCountLine> lines = snapshotLines(command);

		PhysicalCount physicalCount = PhysicalCount.start(PhysicalCountId.of(UUID.randomUUID()), command.scope(),
				command.productGroupId(), command.warehouseId(), command.user(), Instant.now(), lines);

		return physicalCountRepositoryPort.save(physicalCount);
	}

	private List<PhysicalCountLine> snapshotLines(StartPhysicalCountCommand command) {
		List<StockBalance> balances = stockBalanceRepositoryPort.findByWarehouseId(command.warehouseId());

		if (command.scope() == PhysicalCountScope.PARTIAL_BY_GROUP) {
			Set<UUID> productIdsInGroup = productIdsInGroup(command.productGroupId());
			balances = balances.stream().filter(balance -> productIdsInGroup.contains(balance.getProductId())).toList();
		}

		return balances.stream().map(balance -> new PhysicalCountLine(balance.getProductId(), balance.getOnHand()))
				.toList();
	}

	private Set<UUID> productIdsInGroup(String productGroupId) {
		return productRepositoryPort.findAll().stream()
				.filter(product -> product.getClassification() != null
						&& productGroupId.equals(product.getClassification().group()))
				.map(ProductDomain::getId)
				.collect(Collectors.toSet());
	}
}
