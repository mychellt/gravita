package br.gravita.core.ports.outbound.persistence.inventory;

import br.gravita.core.domain.inventory.Lot;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LotRepositoryPort {

	Optional<Lot> findByProductIdAndWarehouseIdAndCode(UUID productId, UUID warehouseId, String code);

	List<Lot> findByExpiryDateLessThanEqual(LocalDate cutoffDate);

	List<Lot> findByExpiryDateLessThanEqualAndWarehouseId(LocalDate cutoffDate, UUID warehouseId);

	Lot save(Lot lot);
}
