package br.gravita.core.usercases.tax;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.masterdata.Company;
import br.gravita.core.domain.tax.NfeCancellationDeadline;
import br.gravita.core.domain.tax.NfeDocument;
import br.gravita.core.domain.tax.NfeDocumentId;
import br.gravita.core.domain.tax.NfeDocumentStatus;
import br.gravita.core.ports.inbound.tax.CancelNfeCommand;
import br.gravita.core.ports.inbound.tax.CancelNfeUseCase;
import br.gravita.core.ports.outbound.persistence.CompanyRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.NfeRepositoryPort;
import br.gravita.core.ports.outbound.tax.SefazCancellationRequest;
import br.gravita.core.ports.outbound.tax.SubmitToSefazPort;
import java.time.Duration;
import java.time.Instant;

/**
 * UC-M2-04. Rejects up front (before any SEFAZ call) a document that isn't
 * {@code AUTHORIZED} or whose legal cancellation window (24h, or a longer
 * state-specific limit per {@link NfeCancellationDeadline}, counted from
 * authorization) has passed; {@link NfeDocument#cancel} enforces the status
 * rule again as the aggregate's own invariant, same as
 * {@code IssueCorrectionLetterService}/{@code TransmitNfeService} do for
 * their own transitions.
 */
@UseCase
public class CancelNfeService implements CancelNfeUseCase {

	private final NfeRepositoryPort nfeRepositoryPort;
	private final CompanyRepositoryPort companyRepositoryPort;
	private final SubmitToSefazPort submitToSefazPort;

	public CancelNfeService(final NfeRepositoryPort nfeRepositoryPort, final CompanyRepositoryPort companyRepositoryPort,
			final SubmitToSefazPort submitToSefazPort) {
		this.nfeRepositoryPort = nfeRepositoryPort;
		this.companyRepositoryPort = companyRepositoryPort;
		this.submitToSefazPort = submitToSefazPort;
	}

	@Override
	public NfeDocument execute(final CancelNfeCommand command) {
		if (command.justification() == null || command.justification().isBlank()) {
			throw new BusinessRuleException("justification is required");
		}

		final NfeDocumentId id = NfeDocumentId.of(command.nfeDocumentId());
		final NfeDocument document = nfeRepositoryPort.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("NfeDocument not found: " + command.nfeDocumentId()));

		if (document.getStatus() != NfeDocumentStatus.AUTHORIZED) {
			throw new BusinessRuleException(
					"NfeDocument " + id.value() + " is not AUTHORIZED (current status: " + document.getStatus() + ")");
		}

		final Company company = companyRepositoryPort.findById(document.getIssuerCompanyId())
				.orElseThrow(
						() -> new BusinessRuleException("Company not found: " + document.getIssuerCompanyId().value()));

		final Instant now = Instant.now();
		final Duration window = NfeCancellationDeadline.windowFor(company.getState());
		if (now.isAfter(document.getAuthorizedAt().plus(window))) {
			throw new BusinessRuleException("Cancellation window has expired for NfeDocument " + id.value());
		}

		submitToSefazPort.cancel(new SefazCancellationRequest(company.getId(), company.getSefazEnvironment(),
				document.getAccessKey(), document.getSefazProtocol(), command.justification()));

		final NfeDocument cancelled = document.cancel(command.justification(), now);
		return nfeRepositoryPort.save(cancelled);
	}
}
