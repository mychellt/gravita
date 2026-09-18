package br.gravita.tax.application.service;

import br.gravita.tax.application.port.in.CalculateTaxCommand;
import br.gravita.tax.application.port.in.CalculateTaxUseCase;
import br.gravita.tax.application.port.in.TaxCalculationResult;
import br.gravita.tax.application.port.in.TaxItemCommand;
import br.gravita.tax.application.port.out.ProductTaxProfileRepositoryPort;
import br.gravita.tax.application.port.out.TaxRateQuery;
import br.gravita.tax.application.port.out.TaxRuleTableRepositoryPort;
import br.gravita.tax.domain.TaxDomainException;
import br.gravita.tax.domain.model.ItemTaxBreakdown;
import br.gravita.tax.domain.model.ItemTaxInput;
import br.gravita.tax.domain.model.ProductTaxProfile;
import br.gravita.tax.domain.model.TaxCalculationTotals;
import br.gravita.tax.domain.model.TaxOverrideInput;
import br.gravita.tax.domain.model.TaxRateRule;
import br.gravita.tax.domain.service.TaxEngine;

import java.util.ArrayList;
import java.util.List;

/**
 * The single implementation of {@link CalculateTaxUseCase}. Every caller —
 * NFe issuance, NFCe/PDV, NFSe, sales/purchasing previews — goes through
 * this class; there is no per-module variant (UC-M2-02 acceptance criteria).
 */
public class CalculateTaxService implements CalculateTaxUseCase {

	private final ProductTaxProfileRepositoryPort productTaxProfileRepositoryPort;
	private final TaxRuleTableRepositoryPort taxRuleTableRepositoryPort;
	private final TaxEngine taxEngine;

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
