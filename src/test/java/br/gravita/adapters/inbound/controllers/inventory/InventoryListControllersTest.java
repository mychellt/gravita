package br.gravita.adapters.inbound.controllers.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gravita.adapters.inbound.controllers.inventory.controllers.PhysicalCountController;
import br.gravita.adapters.inbound.controllers.inventory.controllers.StockBalanceListController;
import br.gravita.adapters.inbound.controllers.inventory.controllers.StockLotController;
import br.gravita.adapters.inbound.controllers.inventory.controllers.StockMovementController;
import br.gravita.adapters.inbound.controllers.inventory.controllers.StockReservationController;
import br.gravita.core.domain.inventory.DefaultWarehouse;
import br.gravita.core.domain.inventory.PhysicalCount;
import br.gravita.core.domain.inventory.PhysicalCountId;
import br.gravita.core.domain.inventory.PhysicalCountLine;
import br.gravita.core.domain.inventory.PhysicalCountScope;
import br.gravita.core.domain.inventory.PhysicalCountStatus;
import br.gravita.core.domain.inventory.StockMovement;
import br.gravita.core.domain.inventory.StockMovementId;
import br.gravita.core.domain.inventory.StockMovementType;
import br.gravita.core.domain.inventory.StockReservation;
import br.gravita.core.domain.inventory.StockReservationId;
import br.gravita.core.domain.inventory.StockReservationStatus;
import br.gravita.core.domain.shared.Page;
import br.gravita.core.ports.inbound.inventory.ApprovePhysicalCountUseCase;
import br.gravita.core.ports.inbound.inventory.ListLotsQuery;
import br.gravita.core.ports.inbound.inventory.ListLotsUseCase;
import br.gravita.core.ports.inbound.inventory.ListPhysicalCountsQuery;
import br.gravita.core.ports.inbound.inventory.ListPhysicalCountsUseCase;
import br.gravita.core.ports.inbound.inventory.ListStockBalancesQuery;
import br.gravita.core.ports.inbound.inventory.ListStockBalancesUseCase;
import br.gravita.core.ports.inbound.inventory.ListStockMovementsQuery;
import br.gravita.core.ports.inbound.inventory.ListStockMovementsUseCase;
import br.gravita.core.ports.inbound.inventory.ListStockReservationsQuery;
import br.gravita.core.ports.inbound.inventory.ListStockReservationsUseCase;
import br.gravita.core.ports.inbound.inventory.PhysicalCountItemView;
import br.gravita.core.ports.inbound.inventory.RegisterStockEntryUseCase;
import br.gravita.core.ports.inbound.inventory.RegisterStockExitUseCase;
import br.gravita.core.ports.inbound.inventory.ReleaseStockReservationUseCase;
import br.gravita.core.ports.inbound.inventory.ReserveStockUseCase;
import br.gravita.core.ports.inbound.inventory.StartPhysicalCountUseCase;
import br.gravita.core.ports.inbound.inventory.StockBalanceItemView;
import br.gravita.core.ports.inbound.inventory.StockMovementItemView;
import br.gravita.core.ports.inbound.inventory.StockReservationItemView;
import br.gravita.core.ports.inbound.inventory.StockUnitView;
import br.gravita.core.ports.inbound.inventory.SubmitPhysicalCountUseCase;
import br.gravita.core.ports.outbound.security.SessionStorePort;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({StockBalanceListController.class, StockMovementController.class, StockLotController.class,
		StockReservationController.class, PhysicalCountController.class})
class InventoryListControllersTest {

	private static final UUID PRODUCT_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ListStockBalancesUseCase listStockBalancesUseCase;
	@MockitoBean
	private ListStockMovementsUseCase listStockMovementsUseCase;
	@MockitoBean
	private ListLotsUseCase listLotsUseCase;
	@MockitoBean
	private ListStockReservationsUseCase listStockReservationsUseCase;
	@MockitoBean
	private ListPhysicalCountsUseCase listPhysicalCountsUseCase;

	// collaborators of the write endpoints that share these controllers
	@MockitoBean
	private RegisterStockEntryUseCase registerStockEntryUseCase;
	@MockitoBean
	private RegisterStockExitUseCase registerStockExitUseCase;
	@MockitoBean
	private ReserveStockUseCase reserveStockUseCase;
	@MockitoBean
	private ReleaseStockReservationUseCase releaseStockReservationUseCase;
	@MockitoBean
	private StartPhysicalCountUseCase startPhysicalCountUseCase;
	@MockitoBean
	private SubmitPhysicalCountUseCase submitPhysicalCountUseCase;
	@MockitoBean
	private ApprovePhysicalCountUseCase approvePhysicalCountUseCase;
	@MockitoBean
	private SessionStorePort sessionStorePort;

	@Test
	@DisplayName("Balances: returns the page with the product code and forwards productId, page and size")
	void balancesReturnsThePageAndForwardsTheFilters() throws Exception {
		final StockBalanceItemView item = new StockBalanceItemView(PRODUCT_ID, "SKU-A", DefaultWarehouse.ID,
				new BigDecimal("10"), new BigDecimal("4"), BigDecimal.ZERO, new BigDecimal("6"), new BigDecimal("2.5"),
				true);
		when(listStockBalancesUseCase.execute(any())).thenReturn(new Page<>(List.of(item), 1, 5, 6));

		mockMvc.perform(get("/api/inventory/balances").param("productId", PRODUCT_ID.toString())
				.param("page", "1").param("size", "5"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].productCode").value("SKU-A"))
				.andExpect(jsonPath("$.content[0].available").value(6))
				.andExpect(jsonPath("$.content[0].allowNegativeStock").value(true))
				.andExpect(jsonPath("$.page").value(1))
				.andExpect(jsonPath("$.size").value(5))
				.andExpect(jsonPath("$.totalElements").value(6))
				.andExpect(jsonPath("$.totalPages").value(2));

		final ArgumentCaptor<ListStockBalancesQuery> captor = ArgumentCaptor.forClass(ListStockBalancesQuery.class);
		verify(listStockBalancesUseCase).execute(captor.capture());
		assertThat(captor.getValue().productId()).isEqualTo(PRODUCT_ID);
		assertThat(captor.getValue().pageQuery().page()).isEqualTo(1);
		assertThat(captor.getValue().pageQuery().size()).isEqualTo(5);
	}

	@Test
	@DisplayName("Balances: with no params asks for the first page of 20, unfiltered")
	void balancesDefaultsToTheFirstPageOfTwenty() throws Exception {
		when(listStockBalancesUseCase.execute(any())).thenReturn(new Page<>(List.of(), 0, 20, 0));

		mockMvc.perform(get("/api/inventory/balances")).andExpect(status().isOk());

		final ArgumentCaptor<ListStockBalancesQuery> captor = ArgumentCaptor.forClass(ListStockBalancesQuery.class);
		verify(listStockBalancesUseCase).execute(captor.capture());
		assertThat(captor.getValue().productId()).isNull();
		assertThat(captor.getValue().pageQuery().page()).isZero();
		assertThat(captor.getValue().pageQuery().size()).isEqualTo(20);
	}

	@Test
	@DisplayName("Movements: forwards productId, type and the from/to instants")
	void movementsForwardsTheFilters() throws Exception {
		final StockMovement movement = StockMovement.builder().id(new StockMovementId(UUID.randomUUID()))
				.type(StockMovementType.EXIT).productId(PRODUCT_ID).warehouseId(DefaultWarehouse.ID)
				.quantity(new BigDecimal("5")).unitCost(new BigDecimal("3")).originReference("SO-1")
				.user(UUID.randomUUID()).timestamp(Instant.parse("2026-10-08T15:30:00Z")).build();
		when(listStockMovementsUseCase.execute(any()))
				.thenReturn(new Page<>(List.of(new StockMovementItemView(movement, "SKU-A")), 0, 20, 1));

		mockMvc.perform(get("/api/inventory/movements").param("productId", PRODUCT_ID.toString())
				.param("type", "EXIT").param("from", "2026-10-01T00:00:00Z").param("to", "2026-10-31T23:59:59Z"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].type").value("EXIT"))
				.andExpect(jsonPath("$.content[0].productCode").value("SKU-A"))
				.andExpect(jsonPath("$.content[0].originReference").value("SO-1"));

		final ArgumentCaptor<ListStockMovementsQuery> captor = ArgumentCaptor.forClass(ListStockMovementsQuery.class);
		verify(listStockMovementsUseCase).execute(captor.capture());
		assertThat(captor.getValue().productId()).isEqualTo(PRODUCT_ID);
		assertThat(captor.getValue().type()).isEqualTo(StockMovementType.EXIT);
		assertThat(captor.getValue().from()).isEqualTo(Instant.parse("2026-10-01T00:00:00Z"));
		assertThat(captor.getValue().to()).isEqualTo(Instant.parse("2026-10-31T23:59:59Z"));
	}

	@Test
	@DisplayName("Movements: an unknown type or a malformed instant is a 400, not an ignored filter")
	void movementsRejectsMalformedFilters() throws Exception {
		mockMvc.perform(get("/api/inventory/movements").param("type", "BOGUS")).andExpect(status().isBadRequest());
		mockMvc.perform(get("/api/inventory/movements").param("from", "yesterday")).andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("Lots: lists lots by default and serial units when kind=SERIAL")
	void lotsDefaultsToLotsAndAcceptsKindSerial() throws Exception {
		final StockUnitView lot = new StockUnitView(ListLotsQuery.Kind.LOT, UUID.randomUUID(), PRODUCT_ID, "SKU-A",
				DefaultWarehouse.ID, "L-1", LocalDate.of(2027, 1, 15), new BigDecimal("100"), null);
		when(listLotsUseCase.execute(any())).thenReturn(new Page<>(List.of(lot), 0, 20, 1));

		mockMvc.perform(get("/api/inventory/lots"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].kind").value("LOT"))
				.andExpect(jsonPath("$.content[0].code").value("L-1"))
				.andExpect(jsonPath("$.content[0].expiryDate").value("2027-01-15"));
		mockMvc.perform(get("/api/inventory/lots").param("kind", "SERIAL")).andExpect(status().isOk());
		mockMvc.perform(get("/api/inventory/lots").param("kind", "BOGUS")).andExpect(status().isBadRequest());

		final ArgumentCaptor<ListLotsQuery> captor = ArgumentCaptor.forClass(ListLotsQuery.class);
		verify(listLotsUseCase, org.mockito.Mockito.times(2)).execute(captor.capture());
		assertThat(captor.getAllValues()).extracting(ListLotsQuery::kind)
				.containsExactly(ListLotsQuery.Kind.LOT, ListLotsQuery.Kind.SERIAL);
	}

	@Test
	@DisplayName("Reservations: forwards status and productId")
	void reservationsForwardsTheFilters() throws Exception {
		final StockReservation reservation = StockReservation.create(new StockReservationId(UUID.randomUUID()),
				UUID.randomUUID(), PRODUCT_ID, DefaultWarehouse.ID, new BigDecimal("4"));
		when(listStockReservationsUseCase.execute(any()))
				.thenReturn(new Page<>(List.of(new StockReservationItemView(reservation, "SKU-A")), 0, 20, 1));

		mockMvc.perform(get("/api/inventory/reservations").param("status", "ACTIVE")
				.param("productId", PRODUCT_ID.toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].status").value("ACTIVE"))
				.andExpect(jsonPath("$.content[0].productCode").value("SKU-A"));

		final ArgumentCaptor<ListStockReservationsQuery> captor = ArgumentCaptor
				.forClass(ListStockReservationsQuery.class);
		verify(listStockReservationsUseCase).execute(captor.capture());
		assertThat(captor.getValue().status()).isEqualTo(StockReservationStatus.ACTIVE);
		assertThat(captor.getValue().productId()).isEqualTo(PRODUCT_ID);
	}

	@Test
	@DisplayName("Counts: forwards status and returns each count's lines with product code and divergence")
	void countsForwardsTheStatusAndReturnsTheLines() throws Exception {
		final PhysicalCount count = PhysicalCount.start(new PhysicalCountId(UUID.randomUUID()),
				PhysicalCountScope.TOTAL, null, DefaultWarehouse.ID, UUID.randomUUID(),
				Instant.parse("2026-10-14T08:00:00Z"),
				List.of(new PhysicalCountLine(PRODUCT_ID, new BigDecimal("35"), new BigDecimal("33")),
						new PhysicalCountLine(UUID.randomUUID(), new BigDecimal("2"))));
		when(listPhysicalCountsUseCase.execute(any())).thenReturn(new Page<>(
				List.of(new PhysicalCountItemView(count, Map.of(PRODUCT_ID, "SKU-A"))), 0, 20, 1));

		mockMvc.perform(get("/api/inventory/counts").param("status", "IN_PROGRESS"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].status").value("IN_PROGRESS"))
				.andExpect(jsonPath("$.content[0].lines[0].productCode").value("SKU-A"))
				.andExpect(jsonPath("$.content[0].lines[0].divergence").value(-2))
				.andExpect(jsonPath("$.content[0].lines[1].countedQuantity").doesNotExist())
				.andExpect(jsonPath("$.content[0].lines[1].divergence").doesNotExist());

		final ArgumentCaptor<ListPhysicalCountsQuery> captor = ArgumentCaptor.forClass(ListPhysicalCountsQuery.class);
		verify(listPhysicalCountsUseCase).execute(captor.capture());
		assertThat(captor.getValue().status()).isEqualTo(PhysicalCountStatus.IN_PROGRESS);
	}
}
