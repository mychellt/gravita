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
import br.gravita.core.ports.outbound.tax.SefazSubmissionRequest;
import br.gravita.core.ports.outbound.tax.SefazSubmissionResult;
import br.gravita.core.ports.outbound.tax.SubmitToSefazPort;
import br.gravita.core.ports.outbound.tax.TransmissionQueuePort;
import java.util.List;

/**
 * UC-M3-04. {@code originState}/{@code destinationState} are both the
 * issuing company's own state - NFC-e is a walk-in retail sale, so the
 * consumer is always in the same state as the register - but {@link Company}
 * has no structured UF field yet (only a free-text {@code address}), so
 * {@link #ISSUER_STATE} is a documented placeholder until masterdata models
 * one; flagged to the tech lead alongside this PR. Likewise, nothing in M3
 * ties a {@link br.gravita.core.domain.tax.PosSession}/{@link NfceSale} to a
 * specific {@link Company} (the module spec's own domain model omits it), so
 * {@link #resolveIssuingCompany()} treats the system as single-company for
 * now - the same simplification the module spec's UC-01 note implies is
 * still pending activation.
 */
@UseCase
public class IssueNfceService implements IssueNfceUseCase {

	private static final String OPERATION_TYPE = "VENDA_PDV";
	private static final String ISSUER_STATE = "SP";

	private final NfceRepositoryPort nfceRepositoryPort;
	private final CompanyRepositoryPort companyRepositoryPort;
	private final CalculateTaxUseCase calculateTaxUseCase;
	private final AllocateDocumentNumberUseCase allocateDocumentNumberUseCase;
	private final SubmitToSefazPort submitToSefazPort;
	private final TransmissionQueuePort transmissionQueuePort;

	public IssueNfceService(NfceRepositoryPort nfceRepositoryPort, CompanyRepositoryPort companyRepositoryPort,
			CalculateTaxUseCase calculateTaxUseCase, AllocateDocumentNumberUseCase allocateDocumentNumberUseCase,
			SubmitToSefazPort submitToSefazPort, TransmissionQueuePort transmissionQueuePort) {
		this.nfceRepositoryPort = nfceRepositoryPort;
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
			// Guard up front: an already-decided sale must not consume a second
			// document number (AC4) just to fail later in NfceSale.authorize().
			throw new BusinessRuleException(
					"NfceSale " + saleId.value() + " is not DRAFT (current status: " + sale.getStatus() + ")");
		}

		Company company = resolveIssuingCompany();

		// AC3: tax totals always come from the shared engine, never recomputed here.
		TaxCalculationResult taxResult = calculateTaxUseCase.execute(buildTaxCommand(sale, company));

		// AC4: allocated exactly once per sale; concurrent-safety lives in AllocateDocumentNumberService.
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
			// AC1: authorized in real time before the caller returns.
			SefazSubmissionResult result = submitToSefazPort.submit(new SefazSubmissionRequest(company.getId(),
					company.getSefazEnvironment(), onlineAccessKey, sale.getSaleTotal(), taxResult));
			return sale.authorize(documentNumber.series(), documentNumber.number(), onlineAccessKey, result.protocol());
		} catch (SefazUnavailableException unavailable) {
			// AC2: SEFAZ-UF unreachable - queue for later sync instead of blocking the cashier.
			String contingencyAccessKey = accessKey(company, documentNumber, EmissionType.CONTINGENCY);
			NfceSale queued = sale.queueForContingency(documentNumber.series(), documentNumber.number(),
					contingencyAccessKey);
			transmissionQueuePort.enqueue(sale.getId());
			return queued;
		}
	}

	private String accessKey(Company company, DocumentNumber documentNumber, EmissionType emissionType) {
		return NfceAccessKeyGenerator.generate(ISSUER_STATE, company.getCnpj().number(), documentNumber.series(),
				documentNumber.number(), emissionType);
	}

	private CalculateTaxCommand buildTaxCommand(NfceSale sale, Company company) {
		List<TaxItemCommand> items = sale.getItems().stream()
				.map(item -> new TaxItemCommand(item.productId().toString(), item.quantity(), item.unitPrice()))
				.toList();
		// masterdata.TaxRegime and tax.TaxRegime are separate enums with the same
		// values (one per module's package boundary); convert by name at the seam.
		br.gravita.core.domain.tax.TaxRegime taxRegime = br.gravita.core.domain.tax.TaxRegime
				.valueOf(company.getTaxRegime().name());
		return new CalculateTaxCommand(items, ISSUER_STATE, ISSUER_STATE, taxRegime, OPERATION_TYPE, List.of());
	}

	private Company resolveIssuingCompany() {
		return companyRepositoryPort.findAll().stream().filter(c -> c.getParentCompanyId() == null).findFirst()
				.orElseThrow(() -> new BusinessRuleException("No issuing company is registered"));
	}
}
