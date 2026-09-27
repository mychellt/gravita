package br.gravita.core.usercases.tax;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.masterdata.Company;
import br.gravita.core.domain.tax.ItemTaxBreakdown;
import br.gravita.core.domain.tax.NfeDocument;
import br.gravita.core.domain.tax.NfeDocumentId;
import br.gravita.core.domain.tax.NfeDocumentStatus;
import br.gravita.core.domain.tax.NfeItem;
import br.gravita.core.ports.inbound.tax.TaxCalculationResult;
import br.gravita.core.ports.inbound.tax.TransmissionResult;
import br.gravita.core.ports.inbound.tax.TransmitNfeCommand;
import br.gravita.core.ports.inbound.tax.TransmitNfeUseCase;
import br.gravita.core.ports.messaging.SendFiscalDocumentByEmailPort;
import br.gravita.core.ports.outbound.persistence.CompanyRepositoryPort;
import br.gravita.core.ports.outbound.persistence.CustomerRepositoryPort;
import br.gravita.core.ports.outbound.persistence.XmlObjectStoragePort;
import br.gravita.core.ports.outbound.persistence.tax.NfeRepositoryPort;
import br.gravita.core.ports.outbound.tax.DanfeOrientation;
import br.gravita.core.ports.outbound.tax.GenerateDanfePort;
import br.gravita.core.ports.outbound.tax.SefazSubmissionRequest;
import br.gravita.core.ports.outbound.tax.SefazSubmissionResult;
import br.gravita.core.ports.outbound.tax.SubmitToSefazPort;
import br.gravita.core.ports.outbound.tax.TransmissionQueuePort;
import java.util.List;

/**
 * UC-M2-03. Attempts exactly one transmission of a {@code QUEUED}/{@code SENT}
 * {@link NfeDocument}: automatic signing happens inside
 * {@link SubmitToSefazPort}'s adapter (AC1), never here, so this use case
 * never asks for or handles a certificate/credential itself. A SEFAZ timeout
 * ({@link br.gravita.core.domain.tax.SefazUnavailableException}) is left to
 * propagate to the caller rather than handled here - the retry/backoff and
 * SVC-AN/SVC-RS contingency-switch policy (AC2/AC3) belongs to the queue
 * consumer that invokes this use case repeatedly, not to a single attempt.
 */
@UseCase
public class TransmitNfeService implements TransmitNfeUseCase {

	private final NfeRepositoryPort nfeRepositoryPort;
	private final CompanyRepositoryPort companyRepositoryPort;
	private final CustomerRepositoryPort customerRepositoryPort;
	private final SubmitToSefazPort submitToSefazPort;
	private final GenerateDanfePort generateDanfePort;
	private final SendFiscalDocumentByEmailPort sendFiscalDocumentByEmailPort;
	private final XmlObjectStoragePort xmlObjectStoragePort;
	private final TransmissionQueuePort transmissionQueuePort;

	public TransmitNfeService(NfeRepositoryPort nfeRepositoryPort, CompanyRepositoryPort companyRepositoryPort,
			CustomerRepositoryPort customerRepositoryPort, SubmitToSefazPort submitToSefazPort,
			GenerateDanfePort generateDanfePort, SendFiscalDocumentByEmailPort sendFiscalDocumentByEmailPort,
			XmlObjectStoragePort xmlObjectStoragePort, TransmissionQueuePort transmissionQueuePort) {
		this.nfeRepositoryPort = nfeRepositoryPort;
		this.companyRepositoryPort = companyRepositoryPort;
		this.customerRepositoryPort = customerRepositoryPort;
		this.submitToSefazPort = submitToSefazPort;
		this.generateDanfePort = generateDanfePort;
		this.sendFiscalDocumentByEmailPort = sendFiscalDocumentByEmailPort;
		this.xmlObjectStoragePort = xmlObjectStoragePort;
		this.transmissionQueuePort = transmissionQueuePort;
	}

	@Override
	public TransmissionResult execute(TransmitNfeCommand command) {
		NfeDocumentId id = NfeDocumentId.of(command.nfeDocumentId());
		NfeDocument document = nfeRepositoryPort.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("NfeDocument not found: " + command.nfeDocumentId()));

		if (document.getStatus() != NfeDocumentStatus.QUEUED && document.getStatus() != NfeDocumentStatus.SENT) {
			// Guard up front: a document already decided (or never queued) must
			// not be re-submitted to SEFAZ.
			throw new BusinessRuleException(
					"NfeDocument " + id.value() + " is not eligible for transmission (current status: "
							+ document.getStatus() + ")");
		}

		Company company = companyRepositoryPort.findById(document.getIssuerCompanyId())
				.orElseThrow(
						() -> new BusinessRuleException("Company not found: " + document.getIssuerCompanyId().value()));

		NfeDocument sent = document.getStatus() == NfeDocumentStatus.QUEUED ? document.send() : document;
		nfeRepositoryPort.save(sent);

		SefazSubmissionRequest request = new SefazSubmissionRequest(company.getId(), company.getSefazEnvironment(),
				sent.getAccessKey(), sent.getDocumentTotal(), buildTaxResult(sent), sent.isContingencyMode());
		// AC2: SefazUnavailableException is intentionally left to propagate - see
		// the class javadoc.
		SefazSubmissionResult result = submitToSefazPort.submit(request);

		if (result.rejectionReason() != null) {
			NfeDocument rejected = sent.reject(result.rejectionReason());
			nfeRepositoryPort.save(rejected);
			transmissionQueuePort.remove(id.value());
			return new TransmissionResult(NfeDocumentStatus.REJECTED, null, result.rejectionReason());
		}

		NfeDocument authorized = sent.authorize(result.protocol());
		byte[] xml = NfeXmlRenderer.render(authorized, company);
		byte[] danfe = generateDanfePort.generate(authorized, company, DanfeOrientation.PORTRAIT);
		String xmlRef = xmlObjectStoragePort.store(company.getId(), xml);
		String danfeRef = xmlObjectStoragePort.store(company.getId(), danfe);

		NfeDocument saved = nfeRepositoryPort.save(authorized.withStorageRefs(xmlRef, danfeRef));
		transmissionQueuePort.remove(id.value());

		sendAuthorizationEmail(saved, xml, danfe);

		return new TransmissionResult(NfeDocumentStatus.AUTHORIZED, result.protocol(), null);
	}

	/**
	 * AC5: automatic e-mail on authorization. A recipient with no e-mail on
	 * file (see {@link NfeEmailSupport#resolveRecipientEmail}) still leaves
	 * transmission successful - the manual resend action (once a delivery
	 * address is available) covers that case rather than blocking
	 * authorization on it.
	 */
	private void sendAuthorizationEmail(NfeDocument document, byte[] xml, byte[] danfe) {
		NfeEmailSupport.resolveRecipientEmail(document, customerRepositoryPort)
				.ifPresent(email -> sendFiscalDocumentByEmailPort
						.send(NfeEmailSupport.buildEmailRequest(document, email, xml, danfe)));
	}

	private TaxCalculationResult buildTaxResult(NfeDocument document) {
		List<ItemTaxBreakdown> breakdowns = document.getItems().stream().map(NfeItem::taxBreakdown).toList();
		return new TaxCalculationResult(breakdowns, document.getTaxTotals());
	}
}
