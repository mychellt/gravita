package br.gravita.inventory.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.ProductDomain;
import br.gravita.core.domain.ProductStatus;
import br.gravita.core.domain.ProductType;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.inventory.Lot;
import br.gravita.core.domain.inventory.LotId;
import br.gravita.core.domain.inventory.SerialUnit;
import br.gravita.core.domain.inventory.StockBalance;
import br.gravita.core.domain.inventory.StockBalanceId;
import br.gravita.core.domain.inventory.StockMovement;
import br.gravita.core.domain.inventory.StockMovementType;
import br.gravita.core.ports.inbound.inventory.RegisterStockEntryCommand;
import br.gravita.core.ports.inbound.inventory.RegisterStockEntryCommand.LotDetails;
import br.gravita.core.ports.outbound.persistence.ProductRepositoryPort;
import br.gravita.core.ports.outbound.persistence.inventory.LotRepositoryPort;
import br.gravita.core.ports.outbound.persistence.inventory.SerialUnitRepositoryPort;
import br.gravita.core.ports.outbound.persistence.inventory.StockBalanceRepositoryPort;
import br.gravita.core.ports.outbound.persistence.inventory.StockMovementRepositoryPort;
import br.gravita.core.usercases.inventory.RegisterStockEntryService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RegisterStockEntryServiceTest {

	@Mock
	private StockBalanceRepositoryPort stockBalanceRepositoryPort;

	@Mock
	private StockMovementRepositoryPort stockMovementRepositoryPort;

	@Mock
	private LotRepositoryPort lotRepositoryPort;

	@Mock
	private SerialUnitRepositoryPort serialUnitRepositoryPort;

	@Mock
	private ProductRepositoryPort productRepositoryPort;

	private RegisterStockEntryService service;

	private final UUID productId = UUID.randomUUID();
	private final UUID warehouseId = UUID.randomUUID();
	private final UUID userId = UUID.randomUUID();

	@BeforeEach
	void setUp() {
		service = new RegisterStockEntryService(stockBalanceRepositoryPort, stockMovementRepositoryPort,
				lotRepositoryPort, serialUnitRepositoryPort, productRepositoryPort);
	}

	@Test
	void ac1_recalculatesAverageCostAsAWeightedAverageAcrossEntries() {
		when(productRepositoryPort.get(productId)).thenReturn(Optional.of(plainProduct()));
		StockBalance afterFirstEntry = StockBalance.of(StockBalanceId.of(UUID.randomUUID()), productId, warehouseId,
				new BigDecimal("100"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("12.50"));
		when(stockBalanceRepositoryPort.findByProductIdAndWarehouseId(productId, warehouseId))
				.thenReturn(Optional.of(afterFirstEntry));

		service.execute(entryCommand(new BigDecimal("50"), new BigDecimal("14.00")));

		ArgumentCaptor<StockBalance> savedBalance = ArgumentCaptor.forClass(StockBalance.class);
		verify(stockBalanceRepositoryPort).save(savedBalance.capture());
		assertThat(savedBalance.getValue().getOnHand()).isEqualByComparingTo("150");
		// (100*12.50 + 50*14.00) / 150 = 13.00
		assertThat(savedBalance.getValue().getAverageCost()).isEqualByComparingTo("13.00");
	}

	@Test
	void ac1_startsTheAverageCostAtTheEntryUnitCostWhenNoBalanceExistsYet() {
		when(productRepositoryPort.get(productId)).thenReturn(Optional.of(plainProduct()));
		when(stockBalanceRepositoryPort.findByProductIdAndWarehouseId(productId, warehouseId))
				.thenReturn(Optional.empty());

		service.execute(entryCommand(new BigDecimal("100"), new BigDecimal("12.50")));

		ArgumentCaptor<StockBalance> savedBalance = ArgumentCaptor.forClass(StockBalance.class);
		verify(stockBalanceRepositoryPort).save(savedBalance.capture());
		assertThat(savedBalance.getValue().getOnHand()).isEqualByComparingTo("100");
		assertThat(savedBalance.getValue().getAverageCost()).isEqualByComparingTo("12.50");
	}

	@Test
	void ac2_stockMovementRepositoryPortExposesNoUpdateOrDeleteMethod() {
		assertThat(StockMovementRepositoryPort.class.getMethods()).extracting(java.lang.reflect.Method::getName)
				.containsExactly("save");
	}

	@Test
	void ac2_theCreatedMovementIsReturnedAsAnEntryTypeAppendedThroughSaveOnly() {
		when(productRepositoryPort.get(productId)).thenReturn(Optional.of(plainProduct()));
		when(stockBalanceRepositoryPort.findByProductIdAndWarehouseId(productId, warehouseId))
				.thenReturn(Optional.empty());
		when(stockMovementRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		StockMovement movement = service.execute(entryCommand(new BigDecimal("10"), new BigDecimal("5.00")));

		assertThat(movement.getType()).isEqualTo(StockMovementType.ENTRY);
		verify(stockMovementRepositoryPort).save(movement);
	}

	@Test
	void ac3_requiresExpiryDateWhenTheProductHasLotControlActive() {
		when(productRepositoryPort.get(productId)).thenReturn(Optional.of(productWith(true, false)));

		RegisterStockEntryCommand command = new RegisterStockEntryCommand(productId, warehouseId, new BigDecimal("10"),
				new BigDecimal("5.00"), new LotDetails("LOT-1", null), null, "PURCHASE:1", userId);

		assertThatThrownBy(() -> service.execute(command)).isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("lot");
		verify(stockBalanceRepositoryPort, never()).save(any());
	}

	@Test
	void ac3_createsANewLotWhenNoneExistsYetForTheProductWarehouseCode() {
		when(productRepositoryPort.get(productId)).thenReturn(Optional.of(productWith(true, false)));
		when(stockBalanceRepositoryPort.findByProductIdAndWarehouseId(productId, warehouseId))
				.thenReturn(Optional.empty());
		when(lotRepositoryPort.findByProductIdAndWarehouseIdAndCode(productId, warehouseId, "LOT-1"))
				.thenReturn(Optional.empty());

		RegisterStockEntryCommand command = new RegisterStockEntryCommand(productId, warehouseId, new BigDecimal("10"),
				new BigDecimal("5.00"), new LotDetails("LOT-1", LocalDate.now().plusMonths(6)), null, "PURCHASE:1",
				userId);

		service.execute(command);

		ArgumentCaptor<Lot> savedLot = ArgumentCaptor.forClass(Lot.class);
		verify(lotRepositoryPort).save(savedLot.capture());
		assertThat(savedLot.getValue().getCode()).isEqualTo("LOT-1");
		assertThat(savedLot.getValue().getQuantity()).isEqualByComparingTo("10");
	}

	@Test
	void ac3_incrementsAnExistingLotOnARepeatEntry() {
		when(productRepositoryPort.get(productId)).thenReturn(Optional.of(productWith(true, false)));
		when(stockBalanceRepositoryPort.findByProductIdAndWarehouseId(productId, warehouseId))
				.thenReturn(Optional.empty());
		Lot existingLot = Lot.of(LotId.of(UUID.randomUUID()), productId, warehouseId, "LOT-1",
				LocalDate.now().plusMonths(6), new BigDecimal("10"));
		when(lotRepositoryPort.findByProductIdAndWarehouseIdAndCode(productId, warehouseId, "LOT-1"))
				.thenReturn(Optional.of(existingLot));

		RegisterStockEntryCommand command = new RegisterStockEntryCommand(productId, warehouseId, new BigDecimal("5"),
				new BigDecimal("5.00"), new LotDetails("LOT-1", LocalDate.now().plusMonths(6)), null, "PURCHASE:1",
				userId);

		service.execute(command);

		ArgumentCaptor<Lot> savedLot = ArgumentCaptor.forClass(Lot.class);
		verify(lotRepositoryPort).save(savedLot.capture());
		assertThat(savedLot.getValue().getQuantity()).isEqualByComparingTo("15");
	}

	@Test
	void ac4_recordsEachSerialAsAnIndividualUnitWhenSerialControlIsActive() {
		when(productRepositoryPort.get(productId)).thenReturn(Optional.of(productWith(false, true)));
		when(stockBalanceRepositoryPort.findByProductIdAndWarehouseId(productId, warehouseId))
				.thenReturn(Optional.empty());
		when(serialUnitRepositoryPort.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

		RegisterStockEntryCommand command = new RegisterStockEntryCommand(productId, warehouseId, new BigDecimal("3"),
				new BigDecimal("100.00"), null, List.of("SN-1", "SN-2", "SN-3"), "PURCHASE:1", userId);

		service.execute(command);

		ArgumentCaptor<List<SerialUnit>> savedSerials = ArgumentCaptor.forClass(List.class);
		verify(serialUnitRepositoryPort).saveAll(savedSerials.capture());
		assertThat(savedSerials.getValue()).extracting(SerialUnit::getSerialNumber)
				.containsExactlyInAnyOrder("SN-1", "SN-2", "SN-3");
	}

	@Test
	void ac4_rejectsASerialControlledEntryWhoseSerialCountDoesNotMatchQuantity() {
		when(productRepositoryPort.get(productId)).thenReturn(Optional.of(productWith(false, true)));

		RegisterStockEntryCommand command = new RegisterStockEntryCommand(productId, warehouseId, new BigDecimal("3"),
				new BigDecimal("100.00"), null, List.of("SN-1", "SN-2"), "PURCHASE:1", userId);

		assertThatThrownBy(() -> service.execute(command)).isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("serial");
		verify(stockBalanceRepositoryPort, never()).save(any());
	}

	@Test
	void ac5_rejectsAZeroQuantity() {
		RegisterStockEntryCommand command = entryCommand(BigDecimal.ZERO, new BigDecimal("5.00"));

		assertThatThrownBy(() -> service.execute(command)).isInstanceOf(BusinessRuleException.class);
		verify(productRepositoryPort, never()).get(any());
	}

	@Test
	void ac5_rejectsANegativeQuantity() {
		RegisterStockEntryCommand command = entryCommand(new BigDecimal("-1"), new BigDecimal("5.00"));

		assertThatThrownBy(() -> service.execute(command)).isInstanceOf(BusinessRuleException.class);
		verify(productRepositoryPort, never()).get(any());
	}

	@Test
	void rejectsAnEntryForAnUnknownProductWithNotFound() {
		when(productRepositoryPort.get(productId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(entryCommand(new BigDecimal("10"), new BigDecimal("5.00"))))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining(productId.toString());
	}

	private RegisterStockEntryCommand entryCommand(BigDecimal quantity, BigDecimal unitCost) {
		return new RegisterStockEntryCommand(productId, warehouseId, quantity, unitCost, null, null, "PURCHASE:1",
				userId);
	}

	private ProductDomain plainProduct() {
		return productWith(false, false);
	}

	private ProductDomain productWith(boolean lotControl, boolean serialControl) {
		return ProductDomain.builder()
				.id(productId)
				.type(ProductType.SIMPLE)
				.status(ProductStatus.ACTIVE)
				.lotControl(lotControl)
				.serialControl(serialControl)
				.build();
	}
}
