package br.gravita.core.usercases;

import br.gravita.core.ports.inbound.masterdata.ManagePriceTableUseCase;
import br.gravita.core.ports.inbound.masterdata.UpsertPriceTableCommand;
import br.gravita.core.ports.outbound.persistence.PriceTableRepositoryPort;
import br.gravita.core.domain.masterdata.PriceTable;
import br.gravita.core.domain.masterdata.PriceTableId;
import br.gravita.core.domain.masterdata.PriceTableNotFoundException;
import br.gravita.core.annotations.UseCase;
import java.util.UUID;

@UseCase
public class ManagePriceTable implements ManagePriceTableUseCase {

	private final PriceTableRepositoryPort priceTableRepositoryPort;

	public ManagePriceTable(final PriceTableRepositoryPort priceTableRepositoryPort) {
		this.priceTableRepositoryPort = priceTableRepositoryPort;
	}

	private PriceTableId requireExisting(final UUID priceTableId) {
		final PriceTableId id = PriceTableId.of(priceTableId);
		priceTableRepositoryPort.findById(id).orElseThrow(() -> new PriceTableNotFoundException(priceTableId));
		return id;
	}

	@Override
	public PriceTableId execute(final UpsertPriceTableCommand command) {
		final var id = command.priceTableId() == null
				? PriceTableId.of(UUID.randomUUID())
				: requireExisting(command.priceTableId());

		final var priceTable = PriceTable.of(id, command.formation(), command.validFrom(), command.validTo(),
				command.maxDiscountPercent(), command.maxDiscountBehavior(), command.entries());

		final var saved = priceTableRepositoryPort.save(priceTable);
		return saved.getId();
	}
}
