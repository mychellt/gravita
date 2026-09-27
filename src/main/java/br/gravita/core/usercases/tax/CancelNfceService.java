package br.gravita.core.usercases.tax;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.exceptions.UnauthorizedException;
import br.gravita.core.domain.masterdata.Company;
import br.gravita.core.domain.tax.NfceSale;
import br.gravita.core.domain.tax.NfceSaleId;
import br.gravita.core.domain.tax.NfceSaleStatus;
import br.gravita.core.domain.tax.PosSession;
import br.gravita.core.ports.inbound.tax.CancelNfceCommand;
import br.gravita.core.ports.inbound.tax.CancelNfceUseCase;
import br.gravita.core.ports.outbound.persistence.CompanyRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.NfceRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.PosSessionRepositoryPort;
import br.gravita.core.ports.outbound.tax.SefazCancellationRequest;
import br.gravita.core.ports.outbound.tax.SubmitToSefazPort;
import br.gravita.core.ports.outbound.tax.SupervisorAuthorizationPort;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@UseCase
public class CancelNfceService implements CancelNfceUseCase {

	static final Duration CANCELLATION_WINDOW = Duration.ofMinutes(30);

	private final NfceRepositoryPort nfceRepositoryPort;
	private final PosSessionRepositoryPort posSessionRepositoryPort;
	private final CompanyRepositoryPort companyRepositoryPort;
	private final SupervisorAuthorizationPort supervisorAuthorizationPort;
	private final SubmitToSefazPort submitToSefazPort;

	public CancelNfceService(NfceRepositoryPort nfceRepositoryPort, PosSessionRepositoryPort posSessionRepositoryPort,
			CompanyRepositoryPort companyRepositoryPort, SupervisorAuthorizationPort supervisorAuthorizationPort,
			SubmitToSefazPort submitToSefazPort) {
		this.nfceRepositoryPort = nfceRepositoryPort;
		this.posSessionRepositoryPort = posSessionRepositoryPort;
		this.companyRepositoryPort = companyRepositoryPort;
		this.supervisorAuthorizationPort = supervisorAuthorizationPort;
		this.submitToSefazPort = submitToSefazPort;
	}

	@Override
	public void execute(CancelNfceCommand command) {
		if (!supervisorAuthorizationPort.authorize(command.supervisorCredential())) {
			throw new UnauthorizedException("Invalid supervisor credential");
		}

		NfceSaleId saleId = NfceSaleId.of(command.nfceSaleId());
		NfceSale sale = nfceRepositoryPort.findById(saleId)
				.orElseThrow(() -> new ResourceNotFoundException("NfceSale not found: " + command.nfceSaleId()));

		if (sale.getStatus() != NfceSaleStatus.AUTHORIZED) {
			throw new BusinessRuleException(
					"NfceSale " + saleId.value() + " is not AUTHORIZED (current status: " + sale.getStatus() + ")");
		}

		if (!isEligibleForCancellation(sale)) {
			throw new BusinessRuleException("NfceSale " + saleId.value()
					+ " is not eligible for cancellation: it is neither the last sale nor from today");
		}

		if (Instant.now().isAfter(sale.getCreatedAt().plus(CANCELLATION_WINDOW))) {
			throw new BusinessRuleException("Cancellation window has expired for NfceSale " + saleId.value());
		}

		Company company = resolveIssuingCompany(sale);
		submitToSefazPort.cancel(new SefazCancellationRequest(company.getId(), company.getSefazEnvironment(),
				sale.getAccessKey(), sale.getSefazProtocol(), command.reason()));

		nfceRepositoryPort.save(sale.cancel());
	}

	private boolean isEligibleForCancellation(NfceSale sale) {
		boolean isLastSale = nfceRepositoryPort.findMostRecent().map(NfceSale::getId)
				.map(mostRecentId -> mostRecentId.equals(sale.getId())).orElse(false);
		boolean isFromToday = sale.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate()
				.equals(LocalDate.now(ZoneId.systemDefault()));
		return isLastSale || isFromToday;
	}

	private Company resolveIssuingCompany(NfceSale sale) {
		PosSession session = posSessionRepositoryPort.findById(sale.getSessionId())
				.orElseThrow(() -> new BusinessRuleException("PosSession not found: " + sale.getSessionId().value()));
		return companyRepositoryPort.findById(session.getCompanyId())
				.orElseThrow(() -> new BusinessRuleException("Company not found: " + session.getCompanyId().value()));
	}
}
