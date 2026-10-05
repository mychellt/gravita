package br.gravita.core.usercases.reporting;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.exceptions.ForbiddenException;
import br.gravita.core.ports.inbound.reporting.DreQuery;
import br.gravita.core.ports.inbound.reporting.GetManagerialDreUseCase;
import br.gravita.core.ports.inbound.reporting.ManagerialDre;
import br.gravita.core.ports.inbound.reporting.ManagerialDre.CostCenterExpense;
import br.gravita.core.ports.outbound.reporting.FinanceReadModelPort;
import br.gravita.core.ports.outbound.reporting.InventoryReadModelPort;
import br.gravita.core.ports.outbound.reporting.InventoryReadModelPort.SoldQuantity;
import br.gravita.core.ports.outbound.reporting.PermissionCheckPort;
import br.gravita.core.ports.outbound.reporting.SalesReadModelPort;
import br.gravita.core.ports.outbound.reporting.SalesReadModelPort.ProductSales;
import br.gravita.core.ports.outbound.reporting.TaxReadModelPort;
import br.gravita.core.ports.outbound.reporting.TaxReadModelPort.DocumentTaxRecord;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/**
 * Composes the managerial DRE of a month (doc §10.2). Gross revenue is what {@code sales} invoiced; deductions are
 * the sales taxes ({@code tax}'s ICMS, PIS, COFINS and ISS) the authorized fiscal documents of the month state; CMV is
 * what {@code inventory} says the products sold in the month cost; expenses come per cost center from {@code finance}.
 * IPI is not deducted: it is charged on top of the price rather than carved out of it. The net result is derived from
 * those figures, so it reconciles by construction. A cost center filter narrows the expenses only, as revenue,
 * deductions and CMV are not charged to cost centers.
 */
@UseCase
public class GetManagerialDreService implements GetManagerialDreUseCase {

	static final String SCREEN = "dre";

	private final SalesReadModelPort salesReadModelPort;
	private final TaxReadModelPort taxReadModelPort;
	private final InventoryReadModelPort inventoryReadModelPort;
	private final FinanceReadModelPort financeReadModelPort;
	private final PermissionCheckPort permissionCheckPort;

	public GetManagerialDreService(final SalesReadModelPort salesReadModelPort, final TaxReadModelPort taxReadModelPort,
			final InventoryReadModelPort inventoryReadModelPort, final FinanceReadModelPort financeReadModelPort,
			final PermissionCheckPort permissionCheckPort) {
		this.salesReadModelPort = salesReadModelPort;
		this.taxReadModelPort = taxReadModelPort;
		this.inventoryReadModelPort = inventoryReadModelPort;
		this.financeReadModelPort = financeReadModelPort;
		this.permissionCheckPort = permissionCheckPort;
	}

	@Override
	public ManagerialDre execute(final DreQuery query) {
		if (!permissionCheckPort.canView(query.requesterId(), SCREEN)) {
			throw new ForbiddenException("The user's profile cannot view the managerial DRE");
		}
		final LocalDate from = query.period().atDay(1);
		final LocalDate to = query.period().atEndOfMonth();
		final UUID companyId = query.companyId();

		final BigDecimal grossRevenue = salesReadModelPort.dailyRevenue(from, to, companyId).values().stream()
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		final BigDecimal deductions = deductions(taxReadModelPort.authorizedDocumentTaxes(from, to));
		final BigDecimal cmv = inventoryReadModelPort.costOfGoodsSold(
				salesReadModelPort.productSales(from, to, companyId).stream().map(GetManagerialDreService::sold).toList());
		final List<CostCenterExpense> expenses = expenses(
				financeReadModelPort.expensesByCostCenter(from, to, query.costCenter(), companyId));
		final BigDecimal totalExpenses = expenses.stream().map(CostCenterExpense::amount).reduce(BigDecimal.ZERO,
				BigDecimal::add);

		final BigDecimal netResult = grossRevenue.subtract(deductions).subtract(cmv).subtract(totalExpenses);
		return new ManagerialDre(query.period(), query.costCenter(), grossRevenue, deductions, cmv, expenses,
				totalExpenses, netResult);
	}

	private static SoldQuantity sold(final ProductSales sale) {
		return new SoldQuantity(sale.productId(), sale.quantity());
	}

	private static BigDecimal deductions(final List<DocumentTaxRecord> documents) {
		return total(documents, DocumentTaxRecord::icms).add(total(documents, DocumentTaxRecord::pis))
				.add(total(documents, DocumentTaxRecord::cofins)).add(total(documents, DocumentTaxRecord::iss));
	}

	private static BigDecimal total(final List<DocumentTaxRecord> documents, final Function<DocumentTaxRecord, BigDecimal> tax) {
		return documents.stream().map(tax).reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	/** Biggest spender first, the expenses charged to no cost center last. */
	private static List<CostCenterExpense> expenses(final List<FinanceReadModelPort.CostCenterExpense> expenses) {
		final Comparator<CostCenterExpense> order = Comparator
				.comparing((CostCenterExpense expense) -> expense.costCenterId() == null)
				.thenComparing(CostCenterExpense::amount, Comparator.reverseOrder())
				.thenComparing(CostCenterExpense::costCenterId, Comparator.nullsLast(Comparator.naturalOrder()));
		return expenses.stream().map(expense -> new CostCenterExpense(expense.costCenterId(), expense.amount()))
				.sorted(order).toList();
	}
}
