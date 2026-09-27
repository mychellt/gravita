package br.gravita.core.usercases.tax;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.masterdata.Company;
import br.gravita.core.domain.tax.CorrectionLetter;
import br.gravita.core.domain.tax.NfeDocument;
import br.gravita.core.domain.tax.NfeDocumentId;
import br.gravita.core.domain.tax.NfeDocumentStatus;
import br.gravita.core.ports.inbound.tax.IssueCorrectionLetterCommand;
import br.gravita.core.ports.inbound.tax.IssueCorrectionLetterUseCase;
import br.gravita.core.ports.outbound.persistence.CompanyRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.NfeRepositoryPort;
import br.gravita.core.ports.outbound.tax.SefazCorrectionRequest;
import br.gravita.core.ports.outbound.tax.SefazSubmissionResult;
import br.gravita.core.ports.outbound.tax.SubmitToSefazPort;
import java.time.Instant;

/**
 * UC-M2-05. Rejects up front (before any SEFAZ call) a document that isn't
 * {@code AUTHORIZED} or that already carries {@link NfeDocument#MAX_CORRECTION_LETTERS}
 * correction letters; {@link NfeDocument#issueCorrectionLetter} enforces the
 * same rules again as the aggregate's own invariant.
 */
@UseCase
public class IssueCorrectionLetterService implements IssueCorrectionLetterUseCase {

	private final NfeRepositoryPort nfeRepositoryPort;
	private final CompanyRepositoryPort companyRepositoryPort;
	private final SubmitToSefazPort submitToSefazPort;

	public IssueCorrectionLetterService(NfeRepositoryPort nfeRepositoryPort,
			CompanyRepositoryPort companyRepositoryPort, SubmitToSefazPort submitToSefazPort) {
		this.nfeRepositoryPort = nfeRepositoryPort;
		this.companyRepositoryPort = companyRepositoryPort;
		this.submitToSefazPort = submitToSefazPort;
	}

	@Override
	public CorrectionLetter execute(IssueCorrectionLetterCommand command) {
		if (command.text() == null || command.text().isBlank()) {
			throw new BusinessRuleException("text is required");
		}

		NfeDocumentId id = NfeDocumentId.of(command.nfeDocumentId());
		NfeDocument document = nfeRepositoryPort.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("NfeDocument not found: " + command.nfeDocumentId()));

		if (document.getStatus() != NfeDocumentStatus.AUTHORIZED) {
			throw new BusinessRuleException(
					"NfeDocument " + id.value() + " is not AUTHORIZED (current status: " + document.getStatus() + ")");
		}
		if (document.getCorrectionLetters().size() >= NfeDocument.MAX_CORRECTION_LETTERS) {
			throw new BusinessRuleException("NfeDocument " + id.value() + " already has the maximum of "
					+ NfeDocument.MAX_CORRECTION_LETTERS + " correction letters");
		}

		Company company = companyRepositoryPort.findById(document.getIssuerCompanyId())
				.orElseThrow(
						() -> new BusinessRuleException("Company not found: " + document.getIssuerCompanyId().value()));

		int sequenceNumber = document.getCorrectionLetters().size() + 1;
		SefazSubmissionResult result = submitToSefazPort.correct(new SefazCorrectionRequest(company.getId(),
				company.getSefazEnvironment(), document.getAccessKey(), sequenceNumber, command.text()));

		NfeDocument updated = document.issueCorrectionLetter(command.text(), result.protocol(), Instant.now());
		NfeDocument saved = nfeRepositoryPort.save(updated);

		return saved.getCorrectionLetters().get(saved.getCorrectionLetters().size() - 1);
	}
}
