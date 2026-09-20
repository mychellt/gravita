package br.gravita.core.usercases;

import br.gravita.core.ports.inbound.masterdata.ManagePriceTableUseCase;
import br.gravita.core.ports.inbound.masterdata.UpsertPriceTableCommand;
import br.gravita.core.ports.outbound.persistence.PriceTableRepositoryPort;
import br.gravita.core.domain.masterdata.PriceTable;
import br.gravita.core.domain.masterdata.PriceTableId;
import br.gravita.core.domain.masterdata.PriceTableNotFoundException;
import br.gravita.core.domain.shared.UseCase;
import java.util.UUID;

@UseCase
public class ManagePriceTable implements ManagePriceTableUseCase {

	private final PriceTableRepositoryPort priceTableRepositoryPort;

	public ManagePriceTable(PriceTableRepositoryPort priceTableRepositoryPort) {
		this.priceTableRepositoryPort = priceTableRepositoryPort;
	}

	private PriceTableId requireExisting(UUID priceTableId) {
		PriceTableId id = PriceTableId.of(priceTableId);
		priceTableRepositoryPort.findById(id).orElseThrow(() -> new PriceTableNotFoundException(priceTableId));
		return id;
	}

	@Override
	public PriceTableId execute(UpsertPriceTableCommand command) {
		var id = command.priceTableId() == null
				? PriceTableId.of(UUID.randomUUID())
				: requireExisting(command.priceTableId());

		var priceTable = PriceTable.of(id, command.formation(), command.validFrom(), command.validTo(),
				command.maxDiscountPercent(), command.maxDiscountBehavior(), command.entries());

		var saved = priceTableRepositoryPort.save(priceTable);
		return saved.getId();
	}
}
