package br.gravita.core.usercases.reporting;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.exceptions.ForbiddenException;
import br.gravita.core.ports.inbound.reporting.AbcClass;
import br.gravita.core.ports.inbound.reporting.AbcCurveEntry;
import br.gravita.core.ports.inbound.reporting.AbcCurveQuery;
import br.gravita.core.ports.inbound.reporting.GetAbcCurveUseCase;
import br.gravita.core.ports.outbound.reporting.PermissionCheckPort;
import br.gravita.core.ports.outbound.reporting.SalesReadModelPort;
import br.gravita.core.ports.outbound.reporting.SalesReadModelPort.CustomerSales;
import br.gravita.core.ports.outbound.reporting.SalesReadModelPort.ProductSales;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Ranks the products or customers invoiced in the period by revenue and classes each one by the cumulative share
 * reached by the entries ranked above it (doc §10.2): the top 80% of revenue is A, the next 15% B, the rest C.
 * Judging by the share <em>before</em> an entry means the one that crosses a threshold keeps the better class,
 * and the top entry is always A even when it alone is above 80%.
 */
@UseCase
public class GetAbcCurveService implements GetAbcCurveUseCase {

	static final String SCREEN = "abc-curve";
	static final BigDecimal CLASS_A_LIMIT = BigDecimal.valueOf(80);
	static final BigDecimal CLASS_B_LIMIT = BigDecimal.valueOf(95);

	private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

	private final SalesReadModelPort salesReadModelPort;
	private final PermissionCheckPort permissionCheckPort;

	public GetAbcCurveService(SalesReadModelPort salesReadModelPort, PermissionCheckPort permissionCheckPort) {
		this.salesReadModelPort = salesReadModelPort;
		this.permissionCheckPort = permissionCheckPort;
	}

	@Override
	public List<AbcCurveEntry> execute(AbcCurveQuery query) {
		if (!permissionCheckPort.canView(query.requesterId(), SCREEN)) {
			throw new ForbiddenException("The user's profile cannot view the ABC curve");
		}
		return classify(revenueByEntity(query));
	}

	private Map<UUID, BigDecimal> revenueByEntity(AbcCurveQuery query) {
		LocalDate from = query.period().atDay(1);
		LocalDate to = query.period().atEndOfMonth();
		return switch (query.type()) {
			case PRODUCT -> salesReadModelPort.productSales(from, to, query.companyId()).stream()
					.collect(Collectors.toMap(ProductSales::productId, ProductSales::value, BigDecimal::add));
			case CUSTOMER -> salesReadModelPort.customerSales(from, to, query.companyId()).stream()
					.collect(Collectors.toMap(CustomerSales::customerId, CustomerSales::value, BigDecimal::add));
		};
	}

	/** Classes are decided on the exact amounts; only the percentages shown are rounded. */
	private List<AbcCurveEntry> classify(Map<UUID, BigDecimal> revenueByEntity) {
		List<Map.Entry<UUID, BigDecimal>> ranking = revenueByEntity.entrySet().stream()
				.filter(entry -> entry.getValue().signum() > 0)
				.sorted(Map.Entry.<UUID, BigDecimal>comparingByValue().reversed().thenComparing(Map.Entry::getKey))
				.toList();
		BigDecimal total = ranking.stream().map(Map.Entry::getValue).reduce(BigDecimal.ZERO, BigDecimal::add);
		List<AbcCurveEntry> curve = new ArrayList<>(ranking.size());
		BigDecimal cumulative = BigDecimal.ZERO;
		for (Map.Entry<UUID, BigDecimal> entry : ranking) {
			AbcClass abcClass = abcClass(cumulative, total);
			cumulative = cumulative.add(entry.getValue());
			curve.add(new AbcCurveEntry(entry.getKey(), entry.getValue(), percent(entry.getValue(), total),
					percent(cumulative, total), abcClass));
		}
		return List.copyOf(curve);
	}

	private AbcClass abcClass(BigDecimal cumulativeBefore, BigDecimal total) {
		BigDecimal scaled = cumulativeBefore.multiply(HUNDRED);
		if (scaled.compareTo(CLASS_A_LIMIT.multiply(total)) < 0) {
			return AbcClass.A;
		}
		return scaled.compareTo(CLASS_B_LIMIT.multiply(total)) < 0 ? AbcClass.B : AbcClass.C;
	}

	private BigDecimal percent(BigDecimal part, BigDecimal whole) {
		return part.multiply(HUNDRED).divide(whole, 2, RoundingMode.HALF_UP);
	}
}
