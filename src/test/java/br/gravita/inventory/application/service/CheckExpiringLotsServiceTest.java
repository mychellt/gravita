package br.gravita.inventory.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.inventory.Lot;
import br.gravita.core.domain.inventory.LotId;
import br.gravita.core.ports.inbound.inventory.CheckExpiringLotsQuery;
import br.gravita.core.ports.inbound.inventory.ExpiringLotView;
import br.gravita.core.ports.outbound.inventory.NotifyExpiringLotPort;
import br.gravita.core.ports.outbound.persistence.inventory.LotRepositoryPort;
import br.gravita.core.usercases.inventory.CheckExpiringLotsService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CheckExpiringLotsServiceTest {

	@Mock
	private LotRepositoryPort lotRepositoryPort;

	@Mock
	private NotifyExpiringLotPort notifyExpiringLotPort;

	private CheckExpiringLotsService service;

	private final UUID productId = UUID.randomUUID();
	private final UUID warehouseId = UUID.randomUUID();

	@BeforeEach
	void setUp() {
		service = new CheckExpiringLotsService(lotRepositoryPort, notifyExpiringLotPort);
	}

	@Test
	void queriesTheRepositoryWithTodayPlusTheConfiguredWithinDaysAsTheCutoff() {
		when(lotRepositoryPort.findByExpiryDateLessThanEqual(any())).thenReturn(List.of());

		service.execute(CheckExpiringLotsQuery.of(15));

		ArgumentCaptor<LocalDate> cutoff = ArgumentCaptor.forClass(LocalDate.class);
		verify(lotRepositoryPort).findByExpiryDateLessThanEqual(cutoff.capture());
		assertThat(cutoff.getValue()).isEqualTo(LocalDate.now().plusDays(15));
	}

	@Test
	void aDifferentWithinDaysProducesADifferentCutoff() {
		when(lotRepositoryPort.findByExpiryDateLessThanEqual(any())).thenReturn(List.of());

		service.execute(CheckExpiringLotsQuery.of(3));

		verify(lotRepositoryPort).findByExpiryDateLessThanEqual(LocalDate.now().plusDays(3));
	}

	@Test
	void includesALotExpiringExactlyOnTheCutoffDate() {
		LocalDate cutoff = LocalDate.now().plusDays(30);
		Lot lot = lotOf(cutoff, "10");
		when(lotRepositoryPort.findByExpiryDateLessThanEqual(cutoff)).thenReturn(List.of(lot));

		List<ExpiringLotView> result = service.execute(CheckExpiringLotsQuery.of(30));

		assertThat(result).hasSize(1);
		assertThat(result.get(0).expiryDate()).isEqualTo(cutoff);
	}

	@Test
	void excludesALotWithZeroRemainingQuantity() {
		LocalDate cutoff = LocalDate.now().plusDays(30);
		Lot expired = lotOf(cutoff, "0");
		when(lotRepositoryPort.findByExpiryDateLessThanEqual(cutoff)).thenReturn(List.of(expired));

		List<ExpiringLotView> result = service.execute(CheckExpiringLotsQuery.of(30));

		assertThat(result).isEmpty();
	}

	@Test
	void filtersByWarehouseWhenAWarehouseIdIsProvided() {
		LocalDate cutoff = LocalDate.now().plusDays(7);
		Lot lot = lotOf(cutoff, "5");
		when(lotRepositoryPort.findByExpiryDateLessThanEqualAndWarehouseId(cutoff, warehouseId)).thenReturn(List.of(lot));

		List<ExpiringLotView> result = service.execute(new CheckExpiringLotsQuery(7, warehouseId));

		assertThat(result).hasSize(1);
		verify(lotRepositoryPort, never()).findByExpiryDateLessThanEqual(any());
	}

	@Test
	void pushesTheResultThroughTheNotificationPort() {
		LocalDate cutoff = LocalDate.now().plusDays(30);
		Lot lot = lotOf(cutoff, "10");
		when(lotRepositoryPort.findByExpiryDateLessThanEqual(cutoff)).thenReturn(List.of(lot));

		List<ExpiringLotView> result = service.execute(CheckExpiringLotsQuery.of(30));

		verify(notifyExpiringLotPort).notify(eq(result));
	}

	@Test
	void rejectsANegativeWithinDays() {
		org.assertj.core.api.Assertions.assertThatThrownBy(() -> new CheckExpiringLotsQuery(-1, null))
				.isInstanceOf(IllegalArgumentException.class);
	}

	private Lot lotOf(LocalDate expiryDate, String quantity) {
		return Lot.of(LotId.of(UUID.randomUUID()), productId, warehouseId, "LOT-1", expiryDate,
				new BigDecimal(quantity));
	}
}
