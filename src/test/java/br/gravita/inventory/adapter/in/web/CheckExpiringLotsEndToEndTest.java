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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

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
	@DisplayName("Returns only lots that expire within the window and still have remaining quantity")
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
	@DisplayName("Includes a lot that expires exactly on the last day of the window")
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
	@DisplayName("Restricts the expiring lots to the given warehouse when a warehouse id is provided")
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
