package br.gravita.core.usercases.reporting;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.exceptions.ForbiddenException;
import br.gravita.core.ports.inbound.reporting.CommissionReportEntry;
import br.gravita.core.ports.inbound.reporting.CommissionReportQuery;
import br.gravita.core.ports.inbound.reporting.GetCommissionReportUseCase;
import br.gravita.core.ports.outbound.reporting.PermissionCheckPort;
import br.gravita.core.ports.outbound.reporting.SalesReadModelPort;
import br.gravita.core.ports.outbound.reporting.SalesReadModelPort.CommissionRecord;
import java.util.Comparator;
import java.util.List;

/**
 * Lists the commissions earned on the orders invoiced in the period (doc §10.2), grouped for payroll: by
 * salesperson, then product, then order, so each salesperson's lines are contiguous and their total is the sum of
 * their amounts. Amounts are the ones stored at calculation time and are never recomputed here.
 */
@UseCase
public class GetCommissionReportService implements GetCommissionReportUseCase {

	static final String SCREEN = "commissions";

	private static final Comparator<CommissionReportEntry> PAYROLL_ORDER = Comparator
			.comparing(CommissionReportEntry::salespersonId).thenComparing(CommissionReportEntry::productId)
			.thenComparing(CommissionReportEntry::orderId);

	private final SalesReadModelPort salesReadModelPort;
	private final PermissionCheckPort permissionCheckPort;

	public GetCommissionReportService(final SalesReadModelPort salesReadModelPort, final PermissionCheckPort permissionCheckPort) {
		this.salesReadModelPort = salesReadModelPort;
		this.permissionCheckPort = permissionCheckPort;
	}

	@Override
	public List<CommissionReportEntry> execute(final CommissionReportQuery query) {
		if (!permissionCheckPort.canView(query.requesterId(), SCREEN)) {
			throw new ForbiddenException("The user's profile cannot view the commission report");
		}
		final List<CommissionRecord> records = salesReadModelPort.commissions(query.period().atDay(1),
				query.period().atEndOfMonth(), query.salesperson());
		return records.stream().map(GetCommissionReportService::toEntry).sorted(PAYROLL_ORDER).toList();
	}

	private static CommissionReportEntry toEntry(final CommissionRecord record) {
		return new CommissionReportEntry(record.salespersonId(), record.productId(), record.orderId(), record.rate(),
				record.amount());
	}
}
