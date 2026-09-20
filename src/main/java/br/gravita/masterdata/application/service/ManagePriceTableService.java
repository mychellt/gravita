package br.gravita.masterdata.application.service;

import br.gravita.masterdata.application.port.in.ManagePriceTableUseCase;
import br.gravita.masterdata.application.port.in.UpsertPriceTableCommand;
import br.gravita.masterdata.application.port.out.PriceTableRepositoryPort;
import br.gravita.masterdata.domain.model.PriceTable;
import br.gravita.masterdata.domain.model.PriceTableId;
import br.gravita.masterdata.domain.model.PriceTableNotFoundException;
import br.gravita.shared.UseCase;
import java.util.UUID;

@UseCase
public class ManagePriceTableService implements ManagePriceTableUseCase {

	private final PriceTableRepositoryPort priceTableRepositoryPort;

	public ManagePriceTableService(PriceTableRepositoryPort priceTableRepositoryPort) {
		this.priceTableRepositoryPort = priceTableRepositoryPort;
	}

	@Override
	public PriceTableId execute(UpsertPriceTableCommand command) {
		PriceTableId id = command.priceTableId() == null
				? PriceTableId.of(UUID.randomUUID())
				: requireExisting(command.priceTableId());

		PriceTable priceTable = PriceTable.of(id, command.formation(), command.validFrom(), command.validTo(),
				command.maxDiscountPercent(), command.maxDiscountBehavior(), command.entries());

		PriceTable saved = priceTableRepositoryPort.save(priceTable);
		return saved.getId();
	}

	private PriceTableId requireExisting(UUID priceTableId) {
		PriceTableId id = PriceTableId.of(priceTableId);
		priceTableRepositoryPort.findById(id).orElseThrow(() -> new PriceTableNotFoundException(priceTableId));
		return id;
	}
}
