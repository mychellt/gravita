package br.gravita.inventory.adapter.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gravita.adapters.outbound.persistence.entities.inventory.StockBalanceJpaEntity;
import br.gravita.adapters.outbound.persistence.entities.inventory.StockReservationJpaEntity;
import br.gravita.adapters.outbound.persistence.repositories.inventory.StockBalanceJpaRepository;
import br.gravita.adapters.outbound.persistence.repositories.inventory.StockReservationJpaRepository;
import br.gravita.core.domain.inventory.StockReservationStatus;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class ReleaseStockReservationEndToEndTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private StockBalanceJpaRepository stockBalanceJpaRepository;

	@Autowired
	private StockReservationJpaRepository stockReservationJpaRepository;

	@Test
	@DisplayName("Releasing a reservation restores available stock and leaves on-hand untouched")
	void releasesTheReservationAndRestoresAvailableStockWithoutTouchingOnHand() throws Exception {
		UUID productId = UUID.randomUUID();
		UUID warehouseId = UUID.randomUUID();
		seedBalance(productId, warehouseId, "50", "30", "0", "9.00");
		UUID reservationId = seedReservation(productId, warehouseId, "30", StockReservationStatus.ACTIVE);

		mockMvc.perform(delete("/api/inventory/reservations/{id}", reservationId))
				.andExpect(status().isNoContent());

		StockBalanceJpaEntity updated = stockBalanceJpaRepository.findByProductIdAndWarehouseId(productId, warehouseId)
				.orElseThrow();
		org.assertj.core.api.Assertions.assertThat(updated.getOnHand()).isEqualByComparingTo("50");
		org.assertj.core.api.Assertions.assertThat(updated.getReserved()).isEqualByComparingTo("0");

		StockReservationJpaEntity releasedReservation = stockReservationJpaRepository.findById(reservationId)
				.orElseThrow();
		org.assertj.core.api.Assertions.assertThat(releasedReservation.getStatus())
				.isEqualTo(StockReservationStatus.RELEASED);
	}

	@Test
	@DisplayName("Releasing a reservation that was already released is rejected")
	void rejectsReleasingAnAlreadyReleasedReservation() throws Exception {
		UUID productId = UUID.randomUUID();
		UUID warehouseId = UUID.randomUUID();
		seedBalance(productId, warehouseId, "50", "0", "0", "9.00");
		UUID reservationId = seedReservation(productId, warehouseId, "30", StockReservationStatus.RELEASED);

		mockMvc.perform(delete("/api/inventory/reservations/{id}", reservationId))
				.andExpect(status().isConflict());
	}

	@Test
	@DisplayName("Releasing a reservation that does not exist is rejected")
	void rejectsReleasingAnUnknownReservation() throws Exception {
		mockMvc.perform(delete("/api/inventory/reservations/{id}", UUID.randomUUID()))
				.andExpect(status().isNotFound());
	}

	private void seedBalance(UUID productId, UUID warehouseId, String onHand, String reserved, String inTransit,
			String averageCost) {
		stockBalanceJpaRepository.save(StockBalanceJpaEntity.builder()
				.id(UUID.randomUUID())
				.productId(productId)
				.warehouseId(warehouseId)
				.onHand(new BigDecimal(onHand))
				.reserved(new BigDecimal(reserved))
				.inTransit(new BigDecimal(inTransit))
				.averageCost(new BigDecimal(averageCost))
				.build());
	}

	private UUID seedReservation(UUID productId, UUID warehouseId, String quantity, StockReservationStatus status) {
		UUID id = UUID.randomUUID();
		stockReservationJpaRepository.save(StockReservationJpaEntity.builder()
				.id(id)
				.orderRef(UUID.randomUUID())
				.productId(productId)
				.warehouseId(warehouseId)
				.quantity(new BigDecimal(quantity))
				.status(status)
				.build());
		return id;
	}
}
