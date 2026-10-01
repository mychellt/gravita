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

	public GetManagerialDreService(SalesReadModelPort salesReadModelPort, TaxReadModelPort taxReadModelPort,
			InventoryReadModelPort inventoryReadModelPort, FinanceReadModelPort financeReadModelPort,
			PermissionCheckPort permissionCheckPort) {
		this.salesReadModelPort = salesReadModelPort;
		this.taxReadModelPort = taxReadModelPort;
		this.inventoryReadModelPort = inventoryReadModelPort;
		this.financeReadModelPort = financeReadModelPort;
		this.permissionCheckPort = permissionCheckPort;
	}

	@Override
	public ManagerialDre execute(DreQuery query) {
		if (!permissionCheckPort.canView(query.requesterId(), SCREEN)) {
			throw new ForbiddenException("The user's profile cannot view the managerial DRE");
		}
		LocalDate from = query.period().atDay(1);
		LocalDate to = query.period().atEndOfMonth();
		UUID companyId = query.companyId();

		BigDecimal grossRevenue = salesReadModelPort.dailyRevenue(from, to, companyId).values().stream()
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal deductions = deductions(taxReadModelPort.authorizedDocumentTaxes(from, to));
		BigDecimal cmv = inventoryReadModelPort.costOfGoodsSold(
				salesReadModelPort.productSales(from, to, companyId).stream().map(GetManagerialDreService::sold).toList());
		List<CostCenterExpense> expenses = expenses(
				financeReadModelPort.expensesByCostCenter(from, to, query.costCenter(), companyId));
		BigDecimal totalExpenses = expenses.stream().map(CostCenterExpense::amount).reduce(BigDecimal.ZERO,
				BigDecimal::add);

		BigDecimal netResult = grossRevenue.subtract(deductions).subtract(cmv).subtract(totalExpenses);
		return new ManagerialDre(query.period(), query.costCenter(), grossRevenue, deductions, cmv, expenses,
				totalExpenses, netResult);
	}

	private static SoldQuantity sold(ProductSales sale) {
		return new SoldQuantity(sale.productId(), sale.quantity());
	}

	private static BigDecimal deductions(List<DocumentTaxRecord> documents) {
		return total(documents, DocumentTaxRecord::icms).add(total(documents, DocumentTaxRecord::pis))
				.add(total(documents, DocumentTaxRecord::cofins)).add(total(documents, DocumentTaxRecord::iss));
	}

	private static BigDecimal total(List<DocumentTaxRecord> documents, Function<DocumentTaxRecord, BigDecimal> tax) {
		return documents.stream().map(tax).reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	/** Biggest spender first, the expenses charged to no cost center last. */
	private static List<CostCenterExpense> expenses(List<FinanceReadModelPort.CostCenterExpense> expenses) {
		Comparator<CostCenterExpense> order = Comparator
				.comparing((CostCenterExpense expense) -> expense.costCenterId() == null)
				.thenComparing(CostCenterExpense::amount, Comparator.reverseOrder())
				.thenComparing(CostCenterExpense::costCenterId, Comparator.nullsLast(Comparator.naturalOrder()));
		return expenses.stream().map(expense -> new CostCenterExpense(expense.costCenterId(), expense.amount()))
				.sorted(order).toList();
	}
}
