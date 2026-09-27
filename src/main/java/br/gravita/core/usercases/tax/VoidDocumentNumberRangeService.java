package br.gravita.core.usercases.tax;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.masterdata.Company;
import br.gravita.core.domain.masterdata.DocumentSeries;
import br.gravita.core.domain.masterdata.DocumentSeriesNotFoundException;
import br.gravita.core.domain.masterdata.FiscalDocumentType;
import br.gravita.core.domain.tax.VoidedNumberRange;
import br.gravita.core.domain.tax.VoidedNumberRangeId;
import br.gravita.core.ports.inbound.tax.VoidDocumentNumberRangeUseCase;
import br.gravita.core.ports.inbound.tax.VoidNumberRangeCommand;
import br.gravita.core.ports.outbound.persistence.CompanyRepositoryPort;
import br.gravita.core.ports.outbound.persistence.DocumentSeriesRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.VoidedNumberRangeRepositoryPort;
import br.gravita.core.ports.outbound.tax.SefazSubmissionResult;
import br.gravita.core.ports.outbound.tax.SefazVoidNumberRangeRequest;
import br.gravita.core.ports.outbound.tax.SubmitToSefazPort;
import java.time.Instant;
import java.util.UUID;

/**
 * UC-M2-06 (Inutilização). The range being voided must belong to an
 * already-configured series (M1-UC04) for the company - voiding numbers
 * against a series that was never set up would leave an audit record with
 * nothing to reconcile against.
 */
@UseCase
public class VoidDocumentNumberRangeService implements VoidDocumentNumberRangeUseCase {

	private final CompanyRepositoryPort companyRepositoryPort;
	private final DocumentSeriesRepositoryPort documentSeriesRepositoryPort;
	private final SubmitToSefazPort submitToSefazPort;
	private final VoidedNumberRangeRepositoryPort voidedNumberRangeRepositoryPort;

	public VoidDocumentNumberRangeService(CompanyRepositoryPort companyRepositoryPort,
			DocumentSeriesRepositoryPort documentSeriesRepositoryPort, SubmitToSefazPort submitToSefazPort,
			VoidedNumberRangeRepositoryPort voidedNumberRangeRepositoryPort) {
		this.companyRepositoryPort = companyRepositoryPort;
		this.documentSeriesRepositoryPort = documentSeriesRepositoryPort;
		this.submitToSefazPort = submitToSefazPort;
		this.voidedNumberRangeRepositoryPort = voidedNumberRangeRepositoryPort;
	}

	@Override
	public VoidedNumberRange execute(VoidNumberRangeCommand command) {
		// AC1: mandatory justification, checked before anything else.
		if (command.justification() == null || command.justification().isBlank()) {
			throw new BusinessRuleException("justification is required to void a document number range");
		}

		Company company = companyRepositoryPort.findById(command.companyId())
				.orElseThrow(() -> new ResourceNotFoundException("Company not found: " + command.companyId().value()));

		DocumentSeries series = documentSeriesRepositoryPort
				.findByCompanyIdAndDocumentType(command.companyId(), FiscalDocumentType.NFE)
				.orElseThrow(
						() -> new DocumentSeriesNotFoundException(command.companyId().value(), FiscalDocumentType.NFE));
		if (!command.series().equals(series.getSeries())) {
			throw new BusinessRuleException(
					"Series " + command.series() + " is not configured for company " + command.companyId().value());
		}

		if (command.endNumber() < command.startNumber()) {
			throw new BusinessRuleException("endNumber must not be less than startNumber");
		}

		// SEFAZ must accept the void before the immutable local record is created.
		SefazSubmissionResult result = submitToSefazPort.voidNumberRange(new SefazVoidNumberRangeRequest(
				command.companyId(), company.getSefazEnvironment(), command.series(), command.startNumber(),
				command.endNumber(), command.justification()));

		VoidedNumberRange voidedNumberRange = VoidedNumberRange.of(VoidedNumberRangeId.of(UUID.randomUUID()),
				command.companyId(), FiscalDocumentType.NFE, command.series(), command.startNumber(),
				command.endNumber(), command.justification(), result.protocol(), Instant.now());

		return voidedNumberRangeRepositoryPort.save(voidedNumberRange);
	}
}
