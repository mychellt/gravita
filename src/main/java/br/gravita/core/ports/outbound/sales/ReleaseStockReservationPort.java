package br.gravita.core.ports.outbound.sales;

import java.util.UUID;

public interface ReleaseStockReservationPort {
	void releaseByOrderRef(UUID orderId);
}
