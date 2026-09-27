package br.gravita.core.usercases.tax;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.masterdata.Company;
import br.gravita.core.domain.masterdata.FiscalDocumentType;
import br.gravita.core.domain.masterdata.DocumentNumber;
import br.gravita.core.domain.tax.NfceAccessKeyGenerator;
import br.gravita.core.domain.tax.NfceAccessKeyGenerator.EmissionType;
import br.gravita.core.domain.tax.NfceSale;
import br.gravita.core.domain.tax.NfceSaleId;
import br.gravita.core.domain.tax.NfceSaleStatus;
import br.gravita.core.domain.tax.PosSession;
import br.gravita.core.domain.tax.SefazUnavailableException;
import br.gravita.core.ports.inbound.masterdata.AllocateDocumentNumberCommand;
import br.gravita.core.ports.inbound.masterdata.AllocateDocumentNumberUseCase;
import br.gravita.core.ports.inbound.tax.CalculateTaxCommand;
import br.gravita.core.ports.inbound.tax.CalculateTaxUseCase;
import br.gravita.core.ports.inbound.tax.IssueNfceCommand;
import br.gravita.core.ports.inbound.tax.IssueNfceUseCase;
import br.gravita.core.ports.inbound.tax.NfceIssuanceResult;
import br.gravita.core.ports.inbound.tax.TaxCalculationResult;
import br.gravita.core.ports.inbound.tax.TaxItemCommand;
import br.gravita.core.ports.outbound.persistence.CompanyRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.NfceRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.PosSessionRepositoryPort;
import br.gravita.core.ports.outbound.tax.SefazSubmissionRequest;
import br.gravita.core.ports.outbound.tax.SefazSubmissionResult;
import br.gravita.core.ports.outbound.tax.SubmitToSefazPort;
import br.gravita.core.ports.outbound.tax.TransmissionQueuePort;
import java.util.List;

@UseCase
public class IssueNfceService implements IssueNfceUseCase {

	private static final String OPERATION_TYPE = "VENDA_PDV";

	private final NfceRepositoryPort nfceRepositoryPort;
	private final PosSessionRepositoryPort posSessionRepositoryPort;
	private final CompanyRepositoryPort companyRepositoryPort;
	private final CalculateTaxUseCase calculateTaxUseCase;
	private final AllocateDocumentNumberUseCase allocateDocumentNumberUseCase;
	private final SubmitToSefazPort submitToSefazPort;
	private final TransmissionQueuePort transmissionQueuePort;

	public IssueNfceService(NfceRepositoryPort nfceRepositoryPort, PosSessionRepositoryPort posSessionRepositoryPort,
			CompanyRepositoryPort companyRepositoryPort, CalculateTaxUseCase calculateTaxUseCase,
			AllocateDocumentNumberUseCase allocateDocumentNumberUseCase, SubmitToSefazPort submitToSefazPort,
			TransmissionQueuePort transmissionQueuePort) {
		this.nfceRepositoryPort = nfceRepositoryPort;
		this.posSessionRepositoryPort = posSessionRepositoryPort;
		this.companyRepositoryPort = companyRepositoryPort;
		this.calculateTaxUseCase = calculateTaxUseCase;
		this.allocateDocumentNumberUseCase = allocateDocumentNumberUseCase;
		this.submitToSefazPort = submitToSefazPort;
		this.transmissionQueuePort = transmissionQueuePort;
	}

	@Override
	public NfceIssuanceResult execute(IssueNfceCommand command) {
		NfceSaleId saleId = NfceSaleId.of(command.nfceSaleId());
		NfceSale sale = nfceRepositoryPort.findById(saleId)
				.orElseThrow(() -> new ResourceNotFoundException("NfceSale not found: " + command.nfceSaleId()));
		if (sale.getStatus() != NfceSaleStatus.DRAFT) {
			throw new BusinessRuleException(
					"NfceSale " + saleId.value() + " is not DRAFT (current status: " + sale.getStatus() + ")");
		}

		Company company = resolveIssuingCompany(sale);

		TaxCalculationResult taxResult = calculateTaxUseCase.execute(buildTaxCommand(sale, company));

		DocumentNumber documentNumber = allocateDocumentNumberUseCase
				.execute(new AllocateDocumentNumberCommand(company.getId(), FiscalDocumentType.NFCE));

		NfceSale issued = attemptIssuance(sale, company, documentNumber, taxResult);
		nfceRepositoryPort.save(issued);

		return new NfceIssuanceResult(issued.getStatus(), issued.getAccessKey(), issued.getSefazProtocol());
	}

	private NfceSale attemptIssuance(NfceSale sale, Company company, DocumentNumber documentNumber,
			TaxCalculationResult taxResult) {
		String onlineAccessKey = accessKey(company, documentNumber, EmissionType.NORMAL);
		try {
			SefazSubmissionResult result = submitToSefazPort.submit(new SefazSubmissionRequest(company.getId(),
					company.getSefazEnvironment(), onlineAccessKey, sale.getSaleTotal(), taxResult));
			return sale.authorize(documentNumber.series(), documentNumber.number(), onlineAccessKey, result.protocol());
		} catch (SefazUnavailableException unavailable) {
			String contingencyAccessKey = accessKey(company, documentNumber, EmissionType.CONTINGENCY);
			NfceSale queued = sale.queueForContingency(documentNumber.series(), documentNumber.number(),
					contingencyAccessKey);
			transmissionQueuePort.enqueue(sale.getId());
			return queued;
		}
	}

	private String accessKey(Company company, DocumentNumber documentNumber, EmissionType emissionType) {
		return NfceAccessKeyGenerator.generate(company.getState(), company.getCnpj().number(), documentNumber.series(),
				documentNumber.number(), emissionType);
	}

	private CalculateTaxCommand buildTaxCommand(NfceSale sale, Company company) {
		List<TaxItemCommand> items = sale.getItems().stream()
				.map(item -> new TaxItemCommand(item.productId().toString(), item.quantity(), item.unitPrice()))
				.toList();
		br.gravita.core.domain.tax.TaxRegime taxRegime = br.gravita.core.domain.tax.TaxRegime
				.valueOf(company.getTaxRegime().name());
		return new CalculateTaxCommand(items, company.getState(), company.getState(), taxRegime, OPERATION_TYPE,
				List.of());
	}

	private Company resolveIssuingCompany(NfceSale sale) {
		PosSession session = posSessionRepositoryPort.findById(sale.getSessionId())
				.orElseThrow(() -> new BusinessRuleException("PosSession not found: " + sale.getSessionId().value()));
		return companyRepositoryPort.findById(session.getCompanyId())
				.orElseThrow(() -> new BusinessRuleException("Company not found: " + session.getCompanyId().value()));
	}
}
