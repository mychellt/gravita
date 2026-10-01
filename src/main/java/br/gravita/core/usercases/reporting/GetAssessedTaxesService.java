package br.gravita.core.usercases.reporting;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.exceptions.ForbiddenException;
import br.gravita.core.ports.inbound.reporting.AssessedTaxSummary;
import br.gravita.core.ports.inbound.reporting.AssessedTaxesQuery;
import br.gravita.core.ports.inbound.reporting.GetAssessedTaxesUseCase;
import br.gravita.core.ports.outbound.reporting.PermissionCheckPort;
import br.gravita.core.ports.outbound.reporting.TaxReadModelPort;
import br.gravita.core.ports.outbound.reporting.TaxReadModelPort.DocumentTaxRecord;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;

/**
 * Sums ICMS, IPI, PIS, COFINS and ISS over the fiscal documents {@code tax} authorized in a month (doc §10.2), for the
 * accountant to review ahead of SPED. Every figure is the sum of what an authorized document states, so the summary
 * reconciles with those documents by construction.
 */
@UseCase
public class GetAssessedTaxesService implements GetAssessedTaxesUseCase {

	static final String SCREEN = "assessed-taxes";

	private final TaxReadModelPort taxReadModelPort;
	private final PermissionCheckPort permissionCheckPort;

	public GetAssessedTaxesService(TaxReadModelPort taxReadModelPort, PermissionCheckPort permissionCheckPort) {
		this.taxReadModelPort = taxReadModelPort;
		this.permissionCheckPort = permissionCheckPort;
	}

	@Override
	public AssessedTaxSummary execute(AssessedTaxesQuery query) {
		if (!permissionCheckPort.canView(query.requesterId(), SCREEN)) {
			throw new ForbiddenException("The user's profile cannot view the assessed taxes");
		}
		LocalDate from = query.period().atDay(1);
		LocalDate to = query.period().atEndOfMonth();
		List<DocumentTaxRecord> documents = taxReadModelPort.authorizedDocumentTaxes(from, to);
		return new AssessedTaxSummary(query.period(), total(documents, DocumentTaxRecord::icms),
				total(documents, DocumentTaxRecord::ipi), total(documents, DocumentTaxRecord::pis),
				total(documents, DocumentTaxRecord::cofins), total(documents, DocumentTaxRecord::iss));
	}

	private static BigDecimal total(List<DocumentTaxRecord> documents, Function<DocumentTaxRecord, BigDecimal> tax) {
		return documents.stream().map(tax).reduce(BigDecimal.ZERO, BigDecimal::add);
	}
}
