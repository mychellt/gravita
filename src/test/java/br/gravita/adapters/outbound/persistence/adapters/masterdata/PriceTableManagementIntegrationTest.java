package br.gravita.adapters.outbound.persistence.adapters.masterdata;

import br.gravita.adapters.outbound.persistence.mappers.masterdata.PriceTablePersistenceMapperImpl;
import br.gravita.core.domain.masterdata.MaxDiscountBehavior;
import br.gravita.core.domain.masterdata.PriceFormation;
import br.gravita.core.domain.masterdata.PriceTable;
import br.gravita.core.domain.masterdata.PriceTableEntry;
import br.gravita.core.domain.masterdata.PriceTableId;
import br.gravita.core.domain.masterdata.PriceTableNotFoundException;
import br.gravita.core.domain.masterdata.ProductOrClassRef;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.masterdata.UpsertPriceTableCommand;
import br.gravita.core.usercases.ManagePriceTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({PriceTableRepositoryAdapter.class, PriceTablePersistenceMapperImpl.class})
class PriceTableManagementIntegrationTest {

	@Autowired
	private PriceTableRepositoryAdapter priceTableRepositoryAdapter;

	@Autowired
	private TestEntityManager entityManager;

	private ManagePriceTable service;

	@BeforeEach
	void setUp() {
		service = new ManagePriceTable(priceTableRepositoryAdapter);
	}

	@Test
	void creatingAFixedPriceTableWithOneEntryPersistsAndReturnsAnId() {
		ProductOrClassRef ref = ProductOrClassRef.product("sku-1");
		PriceTableId id = service.execute(new UpsertPriceTableCommand(null, PriceFormation.FIXED,
				LocalDate.of(2026, 1, 1), null, null, null, List.of(new PriceTableEntry(ref, new BigDecimal("99.90")))));
		flushAndClear();

		assertThat(id).isNotNull();
		PriceTable persisted = priceTableRepositoryAdapter.findById(id).orElseThrow();
		assertThat(persisted.getFormation()).isEqualTo(PriceFormation.FIXED);
		assertThat(persisted.getEntries()).hasSize(1);
		assertThat(persisted.getEntries().get(0).value()).isEqualByComparingTo("99.90");
	}

	@Test
	void patchingAnUnknownIdThrowsNotFound() {
		UUID unknownId = UUID.randomUUID();
		assertThatThrownBy(() -> service.execute(new UpsertPriceTableCommand(unknownId, PriceFormation.FIXED,
				LocalDate.of(2026, 1, 1), null, null, null, List.of())))
				.isInstanceOf(PriceTableNotFoundException.class);
	}

	@Test
	void updatingAnExistingTableFullyReplacesItRatherThanMerging() {
		ProductOrClassRef originalRef = ProductOrClassRef.product("sku-original");
		PriceTableId id = service.execute(new UpsertPriceTableCommand(null, PriceFormation.FIXED,
				LocalDate.of(2026, 1, 1), null, null, null, List.of(new PriceTableEntry(originalRef, BigDecimal.TEN))));
		flushAndClear();

		ProductOrClassRef newRef = ProductOrClassRef.product("sku-replacement");
		service.execute(new UpsertPriceTableCommand(id.value(), PriceFormation.PERCENT_OVER_COST,
				LocalDate.of(2026, 1, 1), null, null, null, List.of(new PriceTableEntry(newRef, BigDecimal.valueOf(20)))));
		flushAndClear();

		PriceTable persisted = priceTableRepositoryAdapter.findById(id).orElseThrow();
		assertThat(persisted.getFormation()).isEqualTo(PriceFormation.PERCENT_OVER_COST);
		assertThat(persisted.getEntries()).hasSize(1);
		assertThat(persisted.getEntries().get(0).ref()).isEqualTo(newRef);
		assertThat(persisted.getEntries().get(0).value()).isEqualByComparingTo("20");
	}

	@Test
	void validToBeforeValidFromIsRejectedWithABusinessRuleMessage() {
		assertThatThrownBy(() -> service.execute(new UpsertPriceTableCommand(null, PriceFormation.FIXED,
				LocalDate.of(2026, 6, 1), LocalDate.of(2026, 1, 1), null, null,
				List.of(new PriceTableEntry(ProductOrClassRef.product("sku-1"), BigDecimal.TEN)))))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("validTo cannot be before validFrom");
	}

	@Test
	void maxDiscountBehaviorBlockAndAlertBothPersistWithoutBlockingCreation() {
		PriceTableId blockId = service.execute(new UpsertPriceTableCommand(null, PriceFormation.FIXED,
				LocalDate.of(2026, 1, 1), null, BigDecimal.valueOf(10), MaxDiscountBehavior.BLOCK,
				List.of(new PriceTableEntry(ProductOrClassRef.product("sku-block"), BigDecimal.TEN))));
		PriceTableId alertId = service.execute(new UpsertPriceTableCommand(null, PriceFormation.FIXED,
				LocalDate.of(2026, 1, 1), null, BigDecimal.valueOf(10), MaxDiscountBehavior.ALERT,
				List.of(new PriceTableEntry(ProductOrClassRef.product("sku-alert"), BigDecimal.TEN))));
		flushAndClear();

		PriceTable block = priceTableRepositoryAdapter.findById(blockId).orElseThrow();
		PriceTable alert = priceTableRepositoryAdapter.findById(alertId).orElseThrow();
		assertThat(block.getMaxDiscountBehavior()).isEqualTo(MaxDiscountBehavior.BLOCK);
		assertThat(alert.getMaxDiscountBehavior()).isEqualTo(MaxDiscountBehavior.ALERT);

		assertThatThrownBy(() -> block.evaluateDiscount(BigDecimal.valueOf(15))).isInstanceOf(BusinessRuleException.class);
		assertThat(alert.evaluateDiscount(BigDecimal.valueOf(15)))
				.isEqualTo(br.gravita.core.domain.masterdata.DiscountCheckResult.ALERT);
	}

	@Test
	void aTableWithAPastValidToIsExcludedFromActiveResolutionWithoutManualDeactivation() {
		PriceTableId id = service.execute(new UpsertPriceTableCommand(null, PriceFormation.FIXED,
				LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31), null, null,
				List.of(new PriceTableEntry(ProductOrClassRef.product("sku-expired"), BigDecimal.TEN))));
		flushAndClear();

		PriceTable persisted = priceTableRepositoryAdapter.findById(id).orElseThrow();
		assertThat(persisted.isActive(LocalDate.now())).isFalse();
	}

	@Test
	void percentFormationsStoreTheRawPercentNotAComputedPriceAtSaveTime() {
		PriceTableId id = service.execute(new UpsertPriceTableCommand(null, PriceFormation.PERCENT_OVER_COST,
				LocalDate.of(2026, 1, 1), null, null, null,
				List.of(new PriceTableEntry(ProductOrClassRef.product("sku-percent"), BigDecimal.valueOf(20)))));
		flushAndClear();

		PriceTable persisted = priceTableRepositoryAdapter.findById(id).orElseThrow();
		assertThat(persisted.getEntries().get(0).value()).isEqualByComparingTo("20");

		BigDecimal resolvedAtCost100 = persisted.resolvePrice(ProductOrClassRef.product("sku-percent"), BigDecimal.valueOf(100), null);
		BigDecimal resolvedAtCost200 = persisted.resolvePrice(ProductOrClassRef.product("sku-percent"), BigDecimal.valueOf(200), null);
		assertThat(resolvedAtCost100).isEqualByComparingTo("120");
		assertThat(resolvedAtCost200).isEqualByComparingTo("240");
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}
}
