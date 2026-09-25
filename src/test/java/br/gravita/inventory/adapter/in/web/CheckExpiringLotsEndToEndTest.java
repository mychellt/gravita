package br.gravita.inventory.adapter.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gravita.core.domain.inventory.Lot;
import br.gravita.core.domain.inventory.LotId;
import br.gravita.core.ports.outbound.inventory.NotifyExpiringLotPort;
import br.gravita.core.ports.outbound.persistence.inventory.LotRepositoryPort;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * GRA-80: end-to-end verification of CheckExpiringLotsUseCase
 * (GET /api/inventory/alerts/expiring-lots) through the real HTTP stack - real
 * controller, real use case, real H2-backed {@link LotRepositoryPort}. {@link
 * NotifyExpiringLotPort} is mocked since its only adapter delivers over real e-mail
 * infra, out of scope for this HTTP-level test - the wiring itself is covered by
 * {@code CheckExpiringLotsServiceTest}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class CheckExpiringLotsEndToEndTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private LotRepositoryPort lotRepositoryPort;

	@MockitoBean
	private NotifyExpiringLotPort notifyExpiringLotPort;

	@Test
	void returnsOnlyLotsWithinTheWindowAndWithRemainingQuantity() throws Exception {
		UUID productId = UUID.randomUUID();
		UUID warehouseId = UUID.randomUUID();
		seedLot(productId, warehouseId, "IN-WINDOW", LocalDate.now().plusDays(10), "5");
		seedLot(productId, warehouseId, "OUT-OF-WINDOW", LocalDate.now().plusDays(40), "5");
		seedLot(productId, warehouseId, "ZERO-QTY", LocalDate.now().plusDays(5), "0");

		mockMvc.perform(get("/api/inventory/alerts/expiring-lots").param("withinDays", "30"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].lotCode").value("IN-WINDOW"));

		Mockito.verify(notifyExpiringLotPort).notify(Mockito.anyList());
	}

	@Test
	void includesALotExpiringExactlyOnTheWindowBoundary() throws Exception {
		UUID productId = UUID.randomUUID();
		UUID warehouseId = UUID.randomUUID();
		seedLot(productId, warehouseId, "ON-BOUNDARY", LocalDate.now().plusDays(15), "3");

		mockMvc.perform(get("/api/inventory/alerts/expiring-lots").param("withinDays", "15"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].lotCode").value("ON-BOUNDARY"));
	}

	@Test
	void filtersByWarehouseWhenProvided() throws Exception {
		UUID productId = UUID.randomUUID();
		UUID matchingWarehouse = UUID.randomUUID();
		UUID otherWarehouse = UUID.randomUUID();
		seedLot(productId, matchingWarehouse, "MATCHING", LocalDate.now().plusDays(5), "2");
		seedLot(productId, otherWarehouse, "OTHER", LocalDate.now().plusDays(5), "2");

		mockMvc.perform(get("/api/inventory/alerts/expiring-lots")
						.param("withinDays", "30")
						.param("warehouseId", matchingWarehouse.toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].lotCode").value("MATCHING"));
	}

	private void seedLot(UUID productId, UUID warehouseId, String code, LocalDate expiryDate, String quantity) {
		lotRepositoryPort.save(Lot.of(LotId.of(UUID.randomUUID()), productId, warehouseId, code, expiryDate,
				new BigDecimal(quantity)));
	}
}
