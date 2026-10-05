package br.gravita.core.usercases.reporting;

import br.gravita.core.ports.inbound.reporting.AbcCurveEntry;
import br.gravita.core.ports.inbound.reporting.AbcCurveType;
import br.gravita.core.ports.inbound.reporting.AssessedTaxSummary;
import br.gravita.core.ports.inbound.reporting.CommissionReportEntry;
import br.gravita.core.ports.inbound.reporting.ExecutiveDashboardView;
import br.gravita.core.ports.inbound.reporting.ExecutiveDashboardView.PeriodComparison;
import br.gravita.core.ports.inbound.reporting.ExecutiveDashboardView.Target;
import br.gravita.core.ports.inbound.reporting.ExecutiveDashboardView.TopProduct;
import br.gravita.core.ports.inbound.reporting.FiscalBookEntry;
import br.gravita.core.ports.inbound.reporting.FiscalBooks;
import br.gravita.core.ports.inbound.reporting.StockTurnoverEntry;
import br.gravita.core.usercases.reporting.ExportLayout.Col;
import br.gravita.core.usercases.reporting.ExportLayout.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * How each report's read model is laid out for export: the same fields its screen shows, in the same order. Entities
 * appear by id, as in the report itself. Layouts are plain functions of the read model, so exporting never reads
 * anything the report did not already return.
 */
final class ExportLayouts {

	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
	private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MM/yyyy");

	private ExportLayouts() {
	}

	static ExportLayout dashboard(final ExecutiveDashboardView view) {
		final List<Table> tables = new ArrayList<>();
		final ExecutiveDashboardView.Revenue revenue = view.revenue();
		tables.add(new Table("Faturamento",
				cols(new Col("Período", 10, false), new Col("Atual", 12, true), new Col("Anterior", 12, true),
						new Col("Variação (%)", 10, true)),
				List.of(row("Dia", revenue.day()), row("Semana", revenue.week()), row("Mês", revenue.month())),
				List.of("Total faturado (fiscal): " + ExportLayout.money(revenue.invoicedTotal())
						+ " | Diferença de conciliação: " + ExportLayout.money(revenue.reconciliationDifference()))));
		final ExecutiveDashboardView.Margin margin = view.margin();
		tables.add(new Table("CMV e Margem",
				cols(new Col("Receita", 12, true), new Col("CMV", 12, true), new Col("Margem bruta", 12, true),
						new Col("Margem bruta (%)", 12, true)),
				List.of(cells(margin.revenue(), margin.cmv(), margin.grossMargin(), margin.grossMarginPercent())),
				List.of()));
		final ExecutiveDashboardView.Delinquency delinquency = view.delinquency();
		tables.add(new Table("Inadimplência",
				cols(new Col("Total em atraso", 12, true), new Col("Títulos", 8, true),
						new Col("Até 30 dias", 12, true), new Col("31 a 60 dias", 12, true),
						new Col("Mais de 60 dias", 12, true)),
				List.of(cells(delinquency.totalOverdue(), delinquency.titleCount(), delinquency.aging().upTo30Days(),
						delinquency.aging().from31To60Days(), delinquency.aging().over60Days())),
				List.of()));
		tables.add(new Table("Estoque crítico",
				cols(new Col("Produto", 20, false), new Col("Motivo", 14, false), new Col("Disponível", 10, true),
						new Col("Mínimo", 10, true), new Col("Depósito", 20, false), new Col("Lote", 10, false),
						new Col("Validade", 10, false)),
				view.criticalStock().stream().map(item -> cells(item.productId(), switch (item.reason()) {
					case BELOW_MINIMUM -> "Abaixo do mínimo";
					case NEAR_EXPIRY -> "Próximo ao vencimento";
				}, item.available(), item.minimum(), item.warehouseId(), item.lotCode(), item.expiryDate())).toList(),
				List.of()));
		tables.add(topProducts("Top produtos por quantidade", view.topProducts().byQuantity()));
		tables.add(topProducts("Top produtos por valor", view.topProducts().byValue()));
		final ExecutiveDashboardView.TargetProgress targets = view.targets();
		final List<List<Object>> targetRows = new ArrayList<>();
		targetRows.add(targetRow("Empresa", targets.company()));
		targets.salespeople().forEach(salesperson -> targetRows.add(targetRow(salesperson.salespersonId(), salesperson.target())));
		tables.add(new Table("Metas de vendas - " + targets.month().format(MONTH),
				cols(new Col("Vendedor", 24, false), new Col("Meta", 12, true), new Col("Realizado", 12, true),
						new Col("Atingido (%)", 10, true)),
				targetRows, List.of()));
		final String period = switch (view.period()) {
			case DAY -> "Dia";
			case WEEK -> "Semana";
			case MONTH -> "Mês";
		};
		return new ExportLayout("Dashboard Executivo",
				List.of("Período de CMV, margem e top produtos: " + period + " (" + view.from().format(DATE) + " a "
						+ view.to().format(DATE) + ")"),
				tables);
	}

	static ExportLayout abcCurve(final AbcCurveType type, final YearMonth period, final List<AbcCurveEntry> curve) {
		final String subject = type == AbcCurveType.PRODUCT ? "Produtos" : "Clientes";
		final List<List<Object>> rows = new ArrayList<>();
		int rank = 1;
		for (final AbcCurveEntry entry : curve) {
			rows.add(cells(rank++, entry.entityId(), entry.revenue(), entry.revenueShare(), entry.cumulativeShare(),
					entry.abcClass().name()));
		}
		return new ExportLayout("Curva ABC - " + subject + " - " + period.format(MONTH), List.of(periodLine(period)),
				List.of(new Table("Curva ABC - " + subject,
						cols(new Col("#", 4, true), new Col(type == AbcCurveType.PRODUCT ? "Produto" : "Cliente", 30, false),
								new Col("Faturamento", 14, true), new Col("Participação (%)", 12, true),
								new Col("Acumulado (%)", 12, true), new Col("Classe", 6, false)),
						rows, List.of("Itens: " + curve.size()))));
	}

	static ExportLayout stockTurnover(final YearMonth period, final List<StockTurnoverEntry> turnover) {
		final long stalled = turnover.stream().filter(StockTurnoverEntry::stalledFlag).count();
		return new ExportLayout("Giro de Estoque - " + period.format(MONTH), List.of(periodLine(period)),
				List.of(new Table("Giro de Estoque",
						cols(new Col("Produto", 30, false), new Col("Giro", 10, true),
								new Col("Item parado", 10, false)),
						turnover.stream().map(entry -> cells(entry.product(), entry.turnoverRate(),
								entry.stalledFlag() ? "Sim" : "Não")).toList(),
						List.of("Produtos: " + turnover.size() + " | Parados: " + stalled))));
	}

	static ExportLayout commissions(final YearMonth period, final List<CommissionReportEntry> commissions) {
		final BigDecimal total = commissions.stream().map(CommissionReportEntry::amount).reduce(BigDecimal.ZERO,
				BigDecimal::add);
		return new ExportLayout("Comissões - " + period.format(MONTH), List.of(periodLine(period)),
				List.of(new Table("Comissões",
						cols(new Col("Vendedor", 24, false), new Col("Produto", 24, false),
								new Col("Pedido", 24, false), new Col("Taxa (%)", 8, true),
								new Col("Comissão", 12, true)),
						commissions.stream().map(entry -> cells(entry.salespersonId(), entry.productId(),
								entry.orderId(), entry.rate(), entry.amount())).toList(),
						List.of("Lançamentos: " + commissions.size() + " | Total de comissões: "
								+ ExportLayout.money(total)))));
	}

	static ExportLayout assessedTaxes(final AssessedTaxSummary summary) {
		return new ExportLayout("Tributos Apurados - " + summary.period().format(MONTH),
				List.of(periodLine(summary.period())),
				List.of(new Table("Tributos Apurados", cols(new Col("Tributo", 20, false), new Col("Valor", 14, true)),
						List.of(cells("ICMS", summary.icms()), cells("IPI", summary.ipi()),
								cells("PIS", summary.pis()), cells("COFINS", summary.cofins()),
								cells("ISS", summary.iss())),
						List.of())));
	}

	/** Only the Excel is laid out here; the PDF of the books is the statutory one {@code GetFiscalBooksUseCase} renders. */
	static ExportLayout fiscalBooks(final FiscalBooks books) {
		final List<Col> documentColumns = cols(new Col("Data", 8, false), new Col("Modelo", 6, false),
				new Col("Série", 5, false), new Col("Número", 8, false), new Col("Chave de acesso", 24, false),
				new Col("Participante", 20, false), new Col("CNPJ/CPF", 12, false), new Col("CFOP", 6, false),
				new Col("Valor total", 10, true), new Col("ICMS", 9, true));
		final List<Col> assessmentColumns = cols(new Col("Natureza", 8, false), new Col("Data", 8, false),
				new Col("Modelo", 6, false), new Col("Série", 5, false), new Col("Número", 8, false),
				new Col("Participante", 22, false), new Col("CFOP", 6, false), new Col("Valor total", 10, true),
				new Col("ICMS", 9, true));
		return new ExportLayout("Livros Fiscais - " + books.period().format(MONTH), List.of(periodLine(books.period())),
				List.of(
						new Table("Livro de Entradas", documentColumns,
								books.entries().stream().map(ExportLayouts::documentRow).toList(),
								List.of(documentTotals(books.entries(), books.icmsCredit()))),
						new Table("Livro de Saídas", documentColumns,
								books.exits().stream().map(ExportLayouts::documentRow).toList(),
								List.of(documentTotals(books.exits(), books.icmsDebit()))),
						new Table("Livro de Apuração do ICMS", assessmentColumns,
								books.icmsAssessment().stream().map(ExportLayouts::assessmentRow).toList(),
								List.of("Débitos (saídas): " + ExportLayout.money(books.icmsDebit()),
										"Créditos (entradas): " + ExportLayout.money(books.icmsCredit()),
										"Saldo (débitos - créditos): " + ExportLayout.money(books.icmsBalance())))));
	}

	private static Table topProducts(final String heading, final List<TopProduct> products) {
		return new Table(heading, cols(new Col("Produto", 30, false), new Col("Quantidade", 12, true),
				new Col("Valor", 12, true)),
				products.stream().map(product -> cells(product.productId(), product.quantity(), product.value()))
						.toList(),
				List.of());
	}

	private static List<Object> row(final String label, final PeriodComparison comparison) {
		return cells(label, comparison.current(), comparison.previous(), comparison.variationPercent());
	}

	private static List<Object> targetRow(final Object who, final Target target) {
		return cells(who, target.valueTarget(), target.valueAchieved(), target.percentComplete());
	}

	private static List<Object> documentRow(final FiscalBookEntry entry) {
		return cells(entry.date(), entry.documentModel(), entry.series(), entry.number(), entry.accessKey(),
				entry.counterpartName(), entry.counterpartDocument(), entry.cfop(), entry.totalValue(),
				entry.icmsValue());
	}

	private static List<Object> assessmentRow(final FiscalBookEntry entry) {
		final String flow = switch (entry.flow()) {
			case EXIT -> "Débito";
			case ENTRY -> "Crédito";
		};
		return cells(flow, entry.date(), entry.documentModel(), entry.series(), entry.number(), entry.counterpartName(), entry.cfop(),
				entry.totalValue(), entry.icmsValue());
	}

	private static String documentTotals(final List<FiscalBookEntry> book, final BigDecimal icms) {
		final BigDecimal total = book.stream().map(FiscalBookEntry::totalValue).reduce(BigDecimal.ZERO, BigDecimal::add);
		return "Documentos: " + book.size() + " | Valor total: " + ExportLayout.money(total) + " | ICMS: "
				+ ExportLayout.money(icms);
	}

	private static String periodLine(final YearMonth period) {
		final LocalDate from = period.atDay(1);
		return "Período: " + from.format(DATE) + " a " + period.atEndOfMonth().format(DATE);
	}

	private static List<Col> cols(final Col... columns) {
		return List.of(columns);
	}

	/** {@code List.of} refuses the null cells of an empty value. */
	private static List<Object> cells(final Object... values) {
		return Collections.unmodifiableList(Arrays.asList(values));
	}
}
