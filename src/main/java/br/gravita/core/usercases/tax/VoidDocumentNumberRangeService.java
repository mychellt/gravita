package br.gravita.core.usercases.tax;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.masterdata.Company;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.CompanyNotFoundException;
import br.gravita.core.domain.tax.VoidedNumberRange;
import br.gravita.core.domain.tax.VoidedNumberRangeId;
import br.gravita.core.ports.inbound.tax.VoidDocumentNumberRangeUseCase;
import br.gravita.core.ports.inbound.tax.VoidNumberRangeCommand;
import br.gravita.core.ports.outbound.persistence.CompanyRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.VoidedNumberRangeRepositoryPort;
import br.gravita.core.ports.outbound.tax.SefazSubmissionResult;
import br.gravita.core.ports.outbound.tax.SefazVoidRangeRequest;
import br.gravita.core.ports.outbound.tax.SubmitToSefazPort;
import java.time.Instant;
import java.util.UUID;

/**
 * UC-M2-06: registers an `Inutilização` for a range of document numbers that
 * were allocated but never used. Reuses GRA-103's {@link SubmitToSefazPort}
 * sandbox adapter rather than a dedicated client - per the ticket, voiding
 * is just another SEFAZ webservice call.
 */
@UseCase
public class VoidDocumentNumberRangeService implements VoidDocumentNumberRangeUseCase {

	private final CompanyRepositoryPort companyRepositoryPort;
	private final SubmitToSefazPort submitToSefazPort;
	private final VoidedNumberRangeRepositoryPort voidedNumberRangeRepositoryPort;

	public VoidDocumentNumberRangeService(CompanyRepositoryPort companyRepositoryPort,
			SubmitToSefazPort submitToSefazPort, VoidedNumberRangeRepositoryPort voidedNumberRangeRepositoryPort) {
		this.companyRepositoryPort = companyRepositoryPort;
		this.submitToSefazPort = submitToSefazPort;
		this.voidedNumberRangeRepositoryPort = voidedNumberRangeRepositoryPort;
	}

	@Override
	public VoidedNumberRange execute(VoidNumberRangeCommand command) {
		// AC1: rejected without a justification, checked before anything else
		// reaches SEFAZ.
		if (command.justification() == null || command.justification().isBlank()) {
			throw new BusinessRuleException("justification is required to void a document number range");
		}
		if (command.startNumber() > command.endNumber()) {
			throw new BusinessRuleException(
					"startNumber (" + command.startNumber() + ") cannot be greater than endNumber ("
							+ command.endNumber() + ")");
		}

		CompanyId companyId = CompanyId.of(command.companyId());
		Company company = companyRepositoryPort.findById(companyId)
				.orElseThrow(() -> new CompanyNotFoundException(command.companyId()));

		// Transmit before persisting locally, same discipline as
		// CancelNfceService: a range must not be recorded as voided unless
		// SEFAZ actually received the Inutilização event.
		SefazSubmissionResult result = submitToSefazPort.voidRange(new SefazVoidRangeRequest(companyId,
				company.getSefazEnvironment(), command.series(), command.startNumber(), command.endNumber(),
				command.justification()));

		VoidedNumberRange voidedNumberRange = VoidedNumberRange.of(VoidedNumberRangeId.of(UUID.randomUUID()),
				companyId, command.series(), command.startNumber(), command.endNumber(), command.justification(),
				result.protocol(), Instant.now());

		return voidedNumberRangeRepositoryPort.save(voidedNumberRange);
	}
}
