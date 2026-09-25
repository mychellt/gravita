package br.gravita.core.ports.inbound.inventory;

import java.util.List;

public interface CheckExpiringLotsUseCase {
	List<ExpiringLotView> execute(CheckExpiringLotsQuery query);
}
