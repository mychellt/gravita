package br.gravita.core.usercases.tax;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.tax.*;
import br.gravita.core.ports.outbound.persistence.tax.ProductTaxProfileRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.TaxRateQuery;
import br.gravita.core.ports.outbound.persistence.tax.TaxRuleTableRepositoryPort;
import br.gravita.core.ports.inbound.tax.CalculateTaxCommand;
import br.gravita.core.ports.inbound.tax.CalculateTaxUseCase;
import br.gravita.core.ports.inbound.tax.TaxCalculationResult;
import br.gravita.core.ports.inbound.tax.TaxItemCommand;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The single implementation of {@link CalculateTaxUseCase}. Every caller —
 * NFe issuance, NFCe/PDV, NFSe, sales/purchasing previews — goes through
 * this class; there is no per-module variant (UC-M2-02 acceptance criteria).
 */
@UseCase
public class CalculateTaxService implements CalculateTaxUseCase {

	private final ProductTaxProfileRepositoryPort productTaxProfileRepositoryPort;
	private final TaxRuleTableRepositoryPort taxRuleTableRepositoryPort;
	private final TaxEngine taxEngine;

	@Autowired
	public CalculateTaxService(ProductTaxProfileRepositoryPort productTaxProfileRepositoryPort,
			TaxRuleTableRepositoryPort taxRuleTableRepositoryPort) {
		this(productTaxProfileRepositoryPort, taxRuleTableRepositoryPort, new TaxEngine());
	}

	CalculateTaxService(ProductTaxProfileRepositoryPort productTaxProfileRepositoryPort,
			TaxRuleTableRepositoryPort taxRuleTableRepositoryPort, TaxEngine taxEngine) {
		this.productTaxProfileRepositoryPort = productTaxProfileRepositoryPort;
		this.taxRuleTableRepositoryPort = taxRuleTableRepositoryPort;
		this.taxEngine = taxEngine;
	}

	@Override
	public TaxCalculationResult execute(CalculateTaxCommand command) {
		List<TaxOverrideInput> overrides = command.overrides().stream()
				.map(o -> new TaxOverrideInput(o.itemIndex(), o.tax(), o.value(), o.justification()))
				.toList();

		List<TaxItemCommand> items = command.items();
		List<ItemTaxBreakdown> breakdowns = new ArrayList<>(items.size());
		for (int itemIndex = 0; itemIndex < items.size(); itemIndex++) {
			int currentItemIndex = itemIndex;
			TaxItemCommand item = items.get(itemIndex);
			ProductTaxProfile profile = productTaxProfileRepositoryPort.findByProductRef(item.productRef())
					.orElseThrow(() -> new TaxDomainException(
							"No tax profile registered for product " + item.productRef()));

			TaxRateQuery query = new TaxRateQuery(profile.ncm(), command.originState(), command.destinationState(),
					command.taxRegime(), command.operationType());
			List<TaxRateRule> applicableRates = taxRuleTableRepositoryPort.findApplicableRates(query);

			ItemTaxInput input = new ItemTaxInput(itemIndex, item.productRef(), item.quantity(), item.unitPrice(),
					applicableRates);
			List<TaxOverrideInput> overridesForItem = overrides.stream()
					.filter(override -> override.itemIndex() == currentItemIndex)
					.toList();

			breakdowns.add(taxEngine.calculate(input, overridesForItem));
		}

		return new TaxCalculationResult(breakdowns, TaxCalculationTotals.from(breakdowns));
	}
}
