package br.gravita.core.usercases.tax;

import static br.gravita.core.usercases.tax.SpedContribuicoesDocuments.money;
import static br.gravita.core.usercases.tax.SpedContribuicoesDocuments.rate;

import br.gravita.core.ports.inbound.tax.SpedContribuicoesFile.Incidence;
import br.gravita.core.usercases.tax.SpedContribuicoesDocument.Item;
import br.gravita.core.usercases.tax.SpedContribuicoesDocument.Levy;
import br.gravita.core.usercases.tax.SpedContribuicoesDocument.Operation;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * Assesses PIS or COFINS over a period's documents: the contribution levied on the sales, the credit the purchases
 * earned (non-cumulative regime only) and what is left to pay once the credit is set against the contribution - up to
 * the contribution; a credit beyond it is carried forward, never refunded here. Every figure is a sum of what the
 * items carry, so the assessment reconciles with the {@code C170} registers by construction.
 */
final class SpedContribuicoesAssessor {

	private static final String CREDITED_CST = "50";

	private SpedContribuicoesAssessor() {
	}

	/**
	 * The contribution levied on one kind of sale: the sales that share a code ({@code COD_CONT}: {@code 01}/{@code
	 * 02} non-cumulative at the general/another rate, {@code 51}/{@code 52} cumulative) and a rate.
	 */
	record Levied(String code, BigDecimal rate, BigDecimal revenue, BigDecimal base, BigDecimal contribution) {
	}

	/** The base of the credit earned on purchases of one nature ({@code NAT_BC_CRED}). */
	record Credited(String nature, BigDecimal base) {
	}

	/** {@code creditRate} is the general rate the credits are earned at, whether or not any was. */
	record Result(SpedTax tax, List<Levied> levied, List<Credited> credited, BigDecimal revenue, BigDecimal base,
			BigDecimal contribution, BigDecimal creditBase, BigDecimal creditRate, BigDecimal credit,
			BigDecimal creditUsed, BigDecimal creditBalance, BigDecimal payable) {

		/** Whether nothing but the general rate is levied, which decides how the company declares its contribution type. */
		boolean onlyGeneralRate(Incidence incidence) {
			return levied.stream().allMatch(entry -> entry.rate().compareTo(rate(tax.basicRate(incidence))) == 0);
		}
	}

	private record LevyKey(String code, BigDecimal rate) {
	}

	private static final class Sums {
		private BigDecimal revenue = money(BigDecimal.ZERO);
		private BigDecimal base = money(BigDecimal.ZERO);
		private BigDecimal contribution = money(BigDecimal.ZERO);
	}

	static Result assess(SpedTax tax, Incidence incidence, List<SpedContribuicoesDocument> documents) {
		Map<LevyKey, Sums> levied = new TreeMap<>(
				Comparator.comparing(LevyKey::code).thenComparing(LevyKey::rate));
		Map<String, BigDecimal> credited = new TreeMap<>();
		BigDecimal credit = money(BigDecimal.ZERO);
		for (SpedContribuicoesDocument document : documents) {
			for (Item item : document.items()) {
				Levy levy = item.levy(tax);
				if (!levy.bearsAmount()) {
					continue;
				}
				if (document.operation() == Operation.EXIT) {
					Sums sums = levied.computeIfAbsent(new LevyKey(code(incidence, levy), levy.rate()),
							key -> new Sums());
					sums.revenue = sums.revenue.add(item.revenue());
					sums.base = sums.base.add(levy.base());
					sums.contribution = sums.contribution.add(levy.amount());
				} else if (CREDITED_CST.equals(levy.cst())) {
					credited.merge(levy.creditNature(), levy.base(), BigDecimal::add);
					credit = credit.add(levy.amount());
				}
			}
		}

		List<Levied> levies = levied.entrySet().stream().map(entry -> new Levied(entry.getKey().code(),
				entry.getKey().rate(), entry.getValue().revenue, entry.getValue().base, entry.getValue().contribution))
				.toList();
		List<Credited> credits = credited.entrySet().stream()
				.map(entry -> new Credited(entry.getKey(), entry.getValue())).toList();

		BigDecimal contribution = sum(levies, Levied::contribution);
		BigDecimal used = credit.min(contribution);
		return new Result(tax, levies, credits, sum(levies, Levied::revenue), sum(levies, Levied::base), contribution,
				sum(credits, Credited::base), rate(tax.basicRate(Incidence.NON_CUMULATIVE)), credit, used,
				credit.subtract(used), contribution.subtract(used));
	}

	/** The levy's {@code COD_CONT}: the general rate takes the basic code of its regime, any other the differentiated one. */
	private static String code(Incidence incidence, Levy levy) {
		boolean general = "01".equals(levy.cst());
		return incidence == Incidence.NON_CUMULATIVE ? (general ? "01" : "02") : (general ? "51" : "52");
	}

	private static <T> BigDecimal sum(List<T> values, Function<T, BigDecimal> amount) {
		return values.stream().map(amount).reduce(money(BigDecimal.ZERO), BigDecimal::add);
	}
}
