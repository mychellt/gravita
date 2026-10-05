package br.gravita.tax.application.service;

import br.gravita.core.usercases.tax.CalculateTaxService;
import br.gravita.core.ports.inbound.tax.CalculateTaxCommand;
import br.gravita.core.ports.inbound.tax.TaxCalculationResult;
import br.gravita.core.ports.inbound.tax.TaxItemCommand;
import br.gravita.core.ports.inbound.tax.TaxOverrideCommand;
import br.gravita.core.ports.outbound.persistence.tax.ProductTaxProfileRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.TaxRateQuery;
import br.gravita.core.ports.outbound.persistence.tax.TaxRuleTableRepositoryPort;
import br.gravita.core.domain.tax.TaxDomainException;
import br.gravita.core.domain.tax.ProductTaxProfile;
import br.gravita.core.domain.tax.TaxRateRule;
import br.gravita.core.domain.tax.TaxRegime;
import br.gravita.core.domain.tax.TaxType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalculateTaxServiceTest {

	@Mock
	private ProductTaxProfileRepositoryPort productTaxProfileRepositoryPort;

	@Mock
	private TaxRuleTableRepositoryPort taxRuleTableRepositoryPort;

	private CalculateTaxService service() {
		return new CalculateTaxService(productTaxProfileRepositoryPort, taxRuleTableRepositoryPort);
	}

	@Test
	@DisplayName("Resolves the product's tax profile and rate table, then delegates the calculation to the tax engine")
	void shouldResolveProductProfileQueryRateTableAndDelegateToTheEngine() {
		when(productTaxProfileRepositoryPort.findByProductRef("PROD-1"))
				.thenReturn(Optional.of(new ProductTaxProfile("PROD-1", "85171231")));
		when(taxRuleTableRepositoryPort.findApplicableRates(new TaxRateQuery("85171231", "SP", "RJ", TaxRegime.LUCRO_REAL, "VENDA")))
				.thenReturn(List.of(new TaxRateRule("85171231", "SP", "RJ", TaxRegime.LUCRO_REAL, "VENDA", TaxType.ICMS,
						new BigDecimal("12"), BigDecimal.ZERO, BigDecimal.ZERO)));

		final CalculateTaxCommand command = new CalculateTaxCommand(
				List.of(new TaxItemCommand("PROD-1", new BigDecimal("10"), new BigDecimal("100.00"))),
				"SP", "RJ", TaxRegime.LUCRO_REAL, "VENDA", List.of());

		final TaxCalculationResult result = service().execute(command);

		assertThat(result.items()).hasSize(1);
		assertThat(result.items().get(0).totalAmount()).isEqualByComparingTo("120.00");
		assertThat(result.totals().byTaxType().get(TaxType.ICMS)).isEqualByComparingTo("120.00");
		assertThat(result.totals().grandTotal()).isEqualByComparingTo("120.00");
	}

	@Test
	@DisplayName("Forwards the caller-supplied operation type verbatim so every caller shares one implementation")
	void shouldForwardTheCallerSuppliedOperationTypeVerbatimSoEveryCallerSharesOneImplementation() {
		when(productTaxProfileRepositoryPort.findByProductRef("PROD-1"))
				.thenReturn(Optional.of(new ProductTaxProfile("PROD-1", "85171231")));
		when(taxRuleTableRepositoryPort.findApplicableRates(any())).thenReturn(List.of());

		final CalculateTaxCommand nfceCommand = new CalculateTaxCommand(
				List.of(new TaxItemCommand("PROD-1", BigDecimal.ONE, BigDecimal.TEN)),
				"SP", "SP", TaxRegime.SIMPLES_NACIONAL, "VENDA_PDV", List.of());

		service().execute(nfceCommand);

		final ArgumentCaptor<TaxRateQuery> captor = ArgumentCaptor.forClass(TaxRateQuery.class);
		verify(taxRuleTableRepositoryPort).findApplicableRates(captor.capture());
		assertThat(captor.getValue().operationType()).isEqualTo("VENDA_PDV");
		assertThat(captor.getValue().regime()).isEqualTo(TaxRegime.SIMPLES_NACIONAL);
	}

	@Test
	@DisplayName("Fails when the product has no registered tax profile")
	void shouldFailWhenProductHasNoRegisteredTaxProfile() {
		when(productTaxProfileRepositoryPort.findByProductRef("UNKNOWN")).thenReturn(Optional.empty());

		final CalculateTaxCommand command = new CalculateTaxCommand(
				List.of(new TaxItemCommand("UNKNOWN", BigDecimal.ONE, BigDecimal.TEN)),
				"SP", "SP", TaxRegime.LUCRO_PRESUMIDO, "VENDA", List.of());

		assertThatThrownBy(() -> service().execute(command)).isInstanceOf(TaxDomainException.class);
	}

	@Test
	@DisplayName("Rejects an override without justification before calling any port")
	void shouldRejectAnOverrideWithoutJustificationBeforeCallingAnyPort() {
		final CalculateTaxCommand command = new CalculateTaxCommand(
				List.of(new TaxItemCommand("PROD-1", BigDecimal.ONE, BigDecimal.TEN)),
				"SP", "SP", TaxRegime.LUCRO_REAL, "VENDA",
				List.of(new TaxOverrideCommand(0, TaxType.ICMS, new BigDecimal("5"), "")));

		assertThatThrownBy(() -> service().execute(command)).isInstanceOf(TaxDomainException.class);
	}
}
