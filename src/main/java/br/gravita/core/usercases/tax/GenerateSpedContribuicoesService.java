package br.gravita.core.usercases.tax;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.masterdata.Company;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.TaxRegime;
import br.gravita.core.domain.tax.InboundNfeStatus;
import br.gravita.core.ports.inbound.tax.GenerateSpedContribuicoesCommand;
import br.gravita.core.ports.inbound.tax.GenerateSpedContribuicoesUseCase;
import br.gravita.core.ports.inbound.tax.SpedContribuicoesFile;
import br.gravita.core.ports.inbound.tax.SpedContribuicoesFile.Assessment;
import br.gravita.core.ports.inbound.tax.SpedContribuicoesFile.Contribution;
import br.gravita.core.ports.inbound.tax.SpedContribuicoesFile.Incidence;
import br.gravita.core.ports.outbound.persistence.CompanyRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.InboundNfeRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.NfeRepositoryPort;
import br.gravita.core.ports.outbound.tax.GenerateSpedFilePort;
import br.gravita.core.usercases.tax.SpedContribuicoesAssessor.Result;
import br.gravita.core.usercases.tax.SpedContribuicoesDocument.Operation;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Generates the EFD Contribuições (PIS/COFINS) of a company's month (UC-M2-12): assesses both contributions over every
 * authorized NFe the company issued and every confirmed NFe it received in the period, and has the file written.
 * Nothing is persisted: the file is derived from the documents {@code tax} already holds.
 *
 * <p>The company's tax regime decides how the contributions are levied. Lucro Real is non-cumulative (PIS 1.65%,
 * COFINS 7.60%, with credits on purchases), Lucro Presumido cumulative (PIS 0.65%, COFINS 3.00%, no credits). A
 * Simples Nacional company pays PIS/COFINS inside its DAS and does not file this obligation, so asking for one is
 * refused rather than answered with an empty file.
 *
 * <p>An NFe belongs to the day SEFAZ authorized it, a received one to the day its supplier issued it, in the clock's
 * time zone - the same rule the fiscal books (UC-M2-13) apply. How each document is levied is in
 * {@link SpedContribuicoesDocuments}, the assessment in {@link SpedContribuicoesAssessor} and the registers in
 * {@link SpedContribuicoesLayout}.
 */
@UseCase
public class GenerateSpedContribuicoesService implements GenerateSpedContribuicoesUseCase {

	private static final DateTimeFormatter FILE_PERIOD = DateTimeFormatter.ofPattern("yyyyMM");
	private static final Comparator<SpedContribuicoesDocument> DOCUMENT_ORDER = Comparator
			.comparing(SpedContribuicoesDocument::date)
			.thenComparing(SpedContribuicoesDocument::series, Comparator.nullsFirst(Comparator.naturalOrder()))
			.thenComparing(SpedContribuicoesDocument::number, GenerateSpedContribuicoesService::compareNumbers)
			.thenComparing(SpedContribuicoesDocument::accessKey, Comparator.nullsFirst(Comparator.naturalOrder()));

	private final CompanyRepositoryPort companyRepositoryPort;
	private final NfeRepositoryPort nfeRepositoryPort;
	private final InboundNfeRepositoryPort inboundNfeRepositoryPort;
	private final GenerateSpedFilePort generateSpedFilePort;
	private final Clock clock;

	@Autowired
	public GenerateSpedContribuicoesService(CompanyRepositoryPort companyRepositoryPort,
			NfeRepositoryPort nfeRepositoryPort, InboundNfeRepositoryPort inboundNfeRepositoryPort,
			GenerateSpedFilePort generateSpedFilePort) {
		this(companyRepositoryPort, nfeRepositoryPort, inboundNfeRepositoryPort, generateSpedFilePort,
				Clock.systemDefaultZone());
	}

	public GenerateSpedContribuicoesService(CompanyRepositoryPort companyRepositoryPort,
			NfeRepositoryPort nfeRepositoryPort, InboundNfeRepositoryPort inboundNfeRepositoryPort,
			GenerateSpedFilePort generateSpedFilePort, Clock clock) {
		this.companyRepositoryPort = companyRepositoryPort;
		this.nfeRepositoryPort = nfeRepositoryPort;
		this.inboundNfeRepositoryPort = inboundNfeRepositoryPort;
		this.generateSpedFilePort = generateSpedFilePort;
		this.clock = clock;
	}

	@Override
	public SpedContribuicoesFile execute(GenerateSpedContribuicoesCommand command) {
		CompanyId companyId = command.companyId();
		Company company = companyRepositoryPort.findById(companyId)
				.orElseThrow(() -> new ResourceNotFoundException("Company not found: " + companyId.value()));
		Incidence incidence = incidenceOf(company);

		YearMonth period = command.period();
		ZoneId zone = clock.getZone();
		Instant from = period.atDay(1).atStartOfDay(zone).toInstant();
		Instant to = period.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant();

		SpedContribuicoesDocuments mapper = new SpedContribuicoesDocuments(incidence, zone);
		List<SpedContribuicoesDocument> documents = new ArrayList<>();
		nfeRepositoryPort.findAuthorizedByCompanyBetween(companyId, from, to)
				.forEach(nfe -> documents.add(mapper.fromIssued(nfe)));
		inboundNfeRepositoryPort.findIssuedByCompanyBetween(companyId, from, to).stream()
				.filter(nfe -> nfe.getStatus() == InboundNfeStatus.CONFIRMED)
				.forEach(nfe -> documents.add(mapper.fromReceived(nfe)));
		documents.sort(DOCUMENT_ORDER);

		Result pis = SpedContribuicoesAssessor.assess(SpedTax.PIS, incidence, documents);
		Result cofins = SpedContribuicoesAssessor.assess(SpedTax.COFINS, incidence, documents);
		byte[] txt = generateSpedFilePort
				.generate(SpedContribuicoesLayout.build(company, period, incidence, documents, pis, cofins));

		long exits = documents.stream().filter(document -> document.operation() == Operation.EXIT).count();
		Assessment assessment = new Assessment(company.getTaxRegime(), incidence, (int) exits,
				documents.size() - (int) exits, contribution(pis), contribution(cofins));
		return new SpedContribuicoesFile(companyId, period,
				"EFD-Contribuicoes-" + company.getCnpj().number() + "-" + period.format(FILE_PERIOD) + ".txt", txt,
				assessment);
	}

	private static Incidence incidenceOf(Company company) {
		TaxRegime regime = company.getTaxRegime();
		return switch (regime) {
			case LUCRO_REAL -> Incidence.NON_CUMULATIVE;
			case LUCRO_PRESUMIDO -> Incidence.CUMULATIVE;
			case SIMPLES_NACIONAL -> throw new BusinessRuleException("Company " + company.getId().value()
					+ " is under the Simples Nacional, which pays PIS/COFINS within its DAS and does not file the"
					+ " EFD Contribuições");
		};
	}

	private static Contribution contribution(Result result) {
		return new Contribution(result.revenue(), result.base(), result.contribution(), result.credit(),
				result.creditUsed(), result.creditBalance(), result.payable());
	}

	/** Numbers compare as numbers where they are ({@code 9} before {@code 10}), as text otherwise; none comes first. */
	private static int compareNumbers(String a, String b) {
		if (a == null || b == null) {
			return a == b ? 0 : a == null ? -1 : 1;
		}
		if (a.chars().allMatch(Character::isDigit) && b.chars().allMatch(Character::isDigit)) {
			int byLength = Integer.compare(a.length(), b.length());
			if (byLength != 0) {
				return byLength;
			}
		}
		return a.compareTo(b);
	}
}
