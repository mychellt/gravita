package br.gravita.tax.domain.service;

import br.gravita.core.usercases.tax.TaxEngine;
import br.gravita.core.domain.tax.TaxDomainException;
import br.gravita.core.domain.tax.ItemTaxBreakdown;
import br.gravita.core.domain.tax.ItemTaxInput;
import br.gravita.core.domain.tax.TaxLineBreakdown;
import br.gravita.core.domain.tax.TaxOverrideInput;
import br.gravita.core.domain.tax.TaxRateRule;
import br.gravita.core.domain.tax.TaxRegime;
import br.gravita.core.domain.tax.TaxType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Fixture-based unit tests for the tax engine, exercised without any
 * repository adapter — matches GRA-32's note to build and test the engine
 * against fixture rate data in parallel with M1.
 */
class TaxEngineTest {

	private final TaxEngine engine = new TaxEngine();

	private static TaxRateRule rule(TaxType taxType, String rate) {
		return rule(taxType, rate, "0", "0");
	}

	private static TaxRateRule rule(TaxType taxType, String rate, String reduction, String mva) {
		return new TaxRateRule("85171231", "SP", "RJ", TaxRegime.LUCRO_REAL, "VENDA", taxType,
				new BigDecimal(rate), new BigDecimal(reduction), new BigDecimal(mva));
	}

	private static ItemTaxInput item(List<TaxRateRule> rates) {
		return new ItemTaxInput(0, "PROD-1", new BigDecimal("10"), new BigDecimal("100.00"), rates);
	}

	private Map<TaxType, TaxLineBreakdown> byType(ItemTaxBreakdown breakdown) {
		return breakdown.taxLines().stream().collect(Collectors.toMap(TaxLineBreakdown::taxType, Function.identity()));
	}

	@Test
	void shouldComputeIcmsIpiPisCofinsAndFcpFromRateTableData() {
		ItemTaxInput input = item(List.of(
				rule(TaxType.ICMS, "12"),
				rule(TaxType.IPI, "5"),
				rule(TaxType.PIS, "1.65"),
				rule(TaxType.COFINS, "7.6"),
				rule(TaxType.FCP, "2")));

		ItemTaxBreakdown breakdown = engine.calculate(input, List.of());
		Map<TaxType, TaxLineBreakdown> lines = byType(breakdown);

		assertThat(lines.get(TaxType.ICMS).finalAmount()).isEqualByComparingTo("120.00");
		assertThat(lines.get(TaxType.IPI).finalAmount()).isEqualByComparingTo("50.00");
		assertThat(lines.get(TaxType.PIS).finalAmount()).isEqualByComparingTo("16.50");
		assertThat(lines.get(TaxType.COFINS).finalAmount()).isEqualByComparingTo("76.00");
		assertThat(lines.get(TaxType.FCP).finalAmount()).isEqualByComparingTo("20.00");
		assertThat(breakdown.totalAmount()).isEqualByComparingTo("282.50");
	}

	@Test
	void shouldComputeIcmsStAsInternalRateOverMvaBaseMinusIcmsProprio() {
		// gross 1000.00; ICMS 12% = 120.00; ST base = 1000 * 1.40 = 1400.00;
		// gross ST = 1400 * 18% = 252.00; ICMS-ST = 252.00 - 120.00 = 132.00
		ItemTaxInput input = item(List.of(
				rule(TaxType.ICMS, "12"),
				rule(TaxType.ICMS_ST, "18", "0", "40")));

		ItemTaxBreakdown breakdown = engine.calculate(input, List.of());
		Map<TaxType, TaxLineBreakdown> lines = byType(breakdown);

		assertThat(lines.get(TaxType.ICMS_ST).base()).isEqualByComparingTo("1400.00");
		assertThat(lines.get(TaxType.ICMS_ST).finalAmount()).isEqualByComparingTo("132.00");
	}

	@Test
	void shouldApplyBaseReductionBeforeComputingRate() {
		// gross 1000.00, 20% base reduction -> effective base 800.00, 12% -> 96.00
		ItemTaxInput input = item(List.of(rule(TaxType.ICMS, "12", "20", "0")));

		ItemTaxBreakdown breakdown = engine.calculate(input, List.of());

		TaxLineBreakdown icms = byType(breakdown).get(TaxType.ICMS);
		assertThat(icms.base()).isEqualByComparingTo("800.00");
		assertThat(icms.finalAmount()).isEqualByComparingTo("96.00");
	}

	@Test
	void shouldSkipTaxTypesWithoutAMatchingRateRow() {
		ItemTaxInput input = item(List.of(rule(TaxType.ICMS, "12")));

		ItemTaxBreakdown breakdown = engine.calculate(input, List.of());

		assertThat(breakdown.taxLines()).hasSize(1);
		assertThat(byType(breakdown)).containsOnlyKeys(TaxType.ICMS);
	}

	@Test
	void shouldApplyManualOverrideWhileRetainingComputedValueForAudit() {
		ItemTaxInput input = item(List.of(rule(TaxType.ICMS, "12")));
		TaxOverrideInput override = new TaxOverrideInput(0, TaxType.ICMS, new BigDecimal("100.00"), "Negotiated with fiscal auditor");

		ItemTaxBreakdown breakdown = engine.calculate(input, List.of(override));

		TaxLineBreakdown icms = byType(breakdown).get(TaxType.ICMS);
		assertThat(icms.computedAmount()).isEqualByComparingTo("120.00");
		assertThat(icms.finalAmount()).isEqualByComparingTo("100.00");
		assertThat(icms.overridden()).isTrue();
		assertThat(icms.overrideJustification()).isEqualTo("Negotiated with fiscal auditor");
		assertThat(breakdown.totalAmount()).isEqualByComparingTo("100.00");
	}

	@Test
	void shouldRejectOverrideWithoutJustification() {
		assertThatThrownBy(() -> new TaxOverrideInput(0, TaxType.ICMS, new BigDecimal("100.00"), " "))
				.isInstanceOf(TaxDomainException.class);
	}

	@Test
	void shouldProduceTheSameResultRegardlessOfWhichRegimeSelectedTheRows() {
		// The engine takes no TaxRegime input at all: whichever rows the
		// rate-table query resolved for a regime are the only thing it sees,
		// so there is no regime-conditional code path to test around.
		ItemTaxInput simplesLikeInput = item(List.of(rule(TaxType.ICMS, "4")));
		ItemTaxInput lucroRealLikeInput = new ItemTaxInput(0, "PROD-1", new BigDecimal("10"), new BigDecimal("100.00"),
				List.of(rule(TaxType.ICMS, "4")));

		ItemTaxBreakdown simplesLikeResult = engine.calculate(simplesLikeInput, List.of());
		ItemTaxBreakdown lucroRealLikeResult = engine.calculate(lucroRealLikeInput, List.of());

		assertThat(simplesLikeResult.totalAmount()).isEqualByComparingTo(lucroRealLikeResult.totalAmount());
	}
}
