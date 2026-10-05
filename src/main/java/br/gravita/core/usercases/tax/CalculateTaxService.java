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

@UseCase
public class CalculateTaxService implements CalculateTaxUseCase {

	private final ProductTaxProfileRepositoryPort productTaxProfileRepositoryPort;
	private final TaxRuleTableRepositoryPort taxRuleTableRepositoryPort;
	private final TaxEngine taxEngine;

	@Autowired
	public CalculateTaxService(final ProductTaxProfileRepositoryPort productTaxProfileRepositoryPort,
			final TaxRuleTableRepositoryPort taxRuleTableRepositoryPort) {
		this(productTaxProfileRepositoryPort, taxRuleTableRepositoryPort, new TaxEngine());
	}

	CalculateTaxService(final ProductTaxProfileRepositoryPort productTaxProfileRepositoryPort,
			final TaxRuleTableRepositoryPort taxRuleTableRepositoryPort, final TaxEngine taxEngine) {
		this.productTaxProfileRepositoryPort = productTaxProfileRepositoryPort;
		this.taxRuleTableRepositoryPort = taxRuleTableRepositoryPort;
		this.taxEngine = taxEngine;
	}

	@Override
	public TaxCalculationResult execute(final CalculateTaxCommand command) {
		final List<TaxOverrideInput> overrides = command.overrides().stream()
				.map(o -> new TaxOverrideInput(o.itemIndex(), o.tax(), o.value(), o.justification()))
				.toList();

		final List<TaxItemCommand> items = command.items();
		final List<ItemTaxBreakdown> breakdowns = new ArrayList<>(items.size());
		for (int itemIndex = 0; itemIndex < items.size(); itemIndex++) {
			final int currentItemIndex = itemIndex;
			final TaxItemCommand item = items.get(itemIndex);
			final ProductTaxProfile profile = productTaxProfileRepositoryPort.findByProductRef(item.productRef())
					.orElseThrow(() -> new TaxDomainException(
							"No tax profile registered for product " + item.productRef()));

			final TaxRateQuery query = new TaxRateQuery(profile.ncm(), command.originState(), command.destinationState(),
					command.taxRegime(), command.operationType());
			final List<TaxRateRule> applicableRates = taxRuleTableRepositoryPort.findApplicableRates(query);

			final ItemTaxInput input = new ItemTaxInput(itemIndex, item.productRef(), item.quantity(), item.unitPrice(),
					applicableRates);
			final List<TaxOverrideInput> overridesForItem = overrides.stream()
					.filter(override -> override.itemIndex() == currentItemIndex)
					.toList();

			breakdowns.add(taxEngine.calculate(input, overridesForItem));
		}

		return new TaxCalculationResult(breakdowns, TaxCalculationTotals.from(breakdowns));
	}
}
