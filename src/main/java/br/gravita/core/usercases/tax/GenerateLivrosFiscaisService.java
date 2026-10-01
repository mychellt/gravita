package br.gravita.core.usercases.tax;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.masterdata.Company;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.FiscalDocumentType;
import br.gravita.core.domain.tax.InboundNfe;
import br.gravita.core.domain.tax.InboundNfeItem;
import br.gravita.core.domain.tax.InboundNfeTotals;
import br.gravita.core.domain.tax.NfeDocument;
import br.gravita.core.domain.tax.TaxType;
import br.gravita.core.domain.tax.VoidedNumberRange;
import br.gravita.core.ports.inbound.tax.GenerateLivrosFiscaisCommand;
import br.gravita.core.ports.inbound.tax.GenerateLivrosFiscaisUseCase;
import br.gravita.core.ports.inbound.tax.LivrosFiscaisBooks;
import br.gravita.core.ports.inbound.tax.LivrosFiscaisBooks.Book;
import br.gravita.core.ports.inbound.tax.LivrosFiscaisBooks.Flow;
import br.gravita.core.ports.inbound.tax.LivrosFiscaisBooks.IcmsAssessment;
import br.gravita.core.ports.inbound.tax.LivrosFiscaisBooks.Line;
import br.gravita.core.ports.inbound.tax.LivrosFiscaisBooks.TaxSummary;
import br.gravita.core.ports.inbound.tax.LivrosFiscaisBooks.Totals;
import br.gravita.core.ports.inbound.tax.LivrosFiscaisBooks.VoidedRange;
import br.gravita.core.ports.inbound.tax.LivrosFiscaisReport;
import br.gravita.core.ports.outbound.persistence.CompanyRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.InboundNfeRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.NfeRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.VoidedNumberRangeRepositoryPort;
import br.gravita.core.ports.outbound.tax.GenerateFiscalBookPort;
import br.gravita.core.ports.outbound.tax.GenerateFiscalBookPort.FiscalBookFiles;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Builds the Entry, Exit and ICMS Assessment books of a company's month, and the ICMS/IPI/PIS/COFINS summary that goes
 * with them (UC-M2-13), and has them rendered as PDF and TXT. Nothing is written: the report is derived from the
 * documents {@code tax} already holds.
 *
 * <p>The entry book is the NFe the company received, by the day their supplier issued them, plus any NFe it issued
 * itself under an entry CFOP (first digit 1, 2 or 3); the exit book is the NFe it issued under an exit CFOP (5, 6 or
 * 7), by the day SEFAZ authorized them. Cancelled and rejected NFe are not booked. The ranges the company voided
 * with SEFAZ in the month ride on the exit book, so a gap in its numbering shows as a voided range instead of
 * disappearing. The ICMS assessment lists the booked documents that stated ICMS, debits first, and its balance is
 * debits minus credits. The summary applies the same rule to each of the four taxes. A document's day is the one it
 * falls on in the clock's time zone.
 */
@UseCase
public class GenerateLivrosFiscaisService implements GenerateLivrosFiscaisUseCase {

	private static final Comparator<Line> DOCUMENT_ORDER = Comparator.comparing(Line::date)
			.thenComparing(Line::series, Comparator.nullsFirst(Comparator.naturalOrder()))
			.thenComparing(Line::number, GenerateLivrosFiscaisService::compareNumbers)
			.thenComparing(Line::accessKey, Comparator.nullsFirst(Comparator.naturalOrder()));

	private final CompanyRepositoryPort companyRepositoryPort;
	private final NfeRepositoryPort nfeRepositoryPort;
	private final InboundNfeRepositoryPort inboundNfeRepositoryPort;
	private final VoidedNumberRangeRepositoryPort voidedNumberRangeRepositoryPort;
	private final GenerateFiscalBookPort generateFiscalBookPort;
	private final Clock clock;

	@Autowired
	public GenerateLivrosFiscaisService(CompanyRepositoryPort companyRepositoryPort,
			NfeRepositoryPort nfeRepositoryPort, InboundNfeRepositoryPort inboundNfeRepositoryPort,
			VoidedNumberRangeRepositoryPort voidedNumberRangeRepositoryPort,
			GenerateFiscalBookPort generateFiscalBookPort) {
		this(companyRepositoryPort, nfeRepositoryPort, inboundNfeRepositoryPort, voidedNumberRangeRepositoryPort,
				generateFiscalBookPort, Clock.systemDefaultZone());
	}

	public GenerateLivrosFiscaisService(CompanyRepositoryPort companyRepositoryPort, NfeRepositoryPort nfeRepositoryPort,
			InboundNfeRepositoryPort inboundNfeRepositoryPort,
			VoidedNumberRangeRepositoryPort voidedNumberRangeRepositoryPort,
			GenerateFiscalBookPort generateFiscalBookPort, Clock clock) {
		this.companyRepositoryPort = companyRepositoryPort;
		this.nfeRepositoryPort = nfeRepositoryPort;
		this.inboundNfeRepositoryPort = inboundNfeRepositoryPort;
		this.voidedNumberRangeRepositoryPort = voidedNumberRangeRepositoryPort;
		this.generateFiscalBookPort = generateFiscalBookPort;
		this.clock = clock;
	}

	@Override
	public LivrosFiscaisReport execute(GenerateLivrosFiscaisCommand command) {
		CompanyId companyId = command.companyId();
		Company company = companyRepositoryPort.findById(companyId)
				.orElseThrow(() -> new ResourceNotFoundException("Company not found: " + companyId.value()));

		YearMonth period = command.period();
		ZoneId zone = clock.getZone();
		Instant from = period.atDay(1).atStartOfDay(zone).toInstant();
		Instant to = period.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant();

		List<Line> entries = new ArrayList<>();
		List<Line> exits = new ArrayList<>();
		for (NfeDocument nfe : nfeRepositoryPort.findAuthorizedByCompanyBetween(companyId, from, to)) {
			Line line = toLine(nfe, zone);
			(line.flow() == Flow.ENTRY ? entries : exits).add(line);
		}
		inboundNfeRepositoryPort.findIssuedByCompanyBetween(companyId, from, to)
				.forEach(nfe -> entries.add(toLine(nfe, zone)));
		entries.sort(DOCUMENT_ORDER);
		exits.sort(DOCUMENT_ORDER);

		List<VoidedRange> voidedRanges = voidedNumberRangeRepositoryPort
				.findByCompanyIdAndVoidedAtBetween(companyId, from, to).stream()
				.filter(range -> range.getDocumentType() == FiscalDocumentType.NFE).map(this::toRange).toList();

		LivrosFiscaisBooks books = new LivrosFiscaisBooks(companyId, company.getCnpj().number(), company.getIe(),
				period, new Book(List.copyOf(entries), List.of(), sum(entries, Line::totalValue)),
				new Book(List.copyOf(exits), voidedRanges, sum(exits, Line::totalValue)),
				assessment(entries, exits), summary(entries, exits));
		FiscalBookFiles files = generateFiscalBookPort.generate(books);
		return new LivrosFiscaisReport(books, files.pdf(), files.txt());
	}

	private static IcmsAssessment assessment(List<Line> entries, List<Line> exits) {
		BigDecimal debit = sum(exits, Line::icmsValue);
		BigDecimal credit = sum(entries, Line::icmsValue);
		List<Line> lines = new ArrayList<>(withIcms(exits));
		lines.addAll(withIcms(entries));
		return new IcmsAssessment(List.copyOf(lines), debit, credit, debit.subtract(credit));
	}

	private static TaxSummary summary(List<Line> entries, List<Line> exits) {
		return new TaxSummary(totals(entries, exits, Line::icmsValue), totals(entries, exits, Line::ipiValue),
				totals(entries, exits, Line::pisValue), totals(entries, exits, Line::cofinsValue));
	}

	private static Totals totals(List<Line> entries, List<Line> exits, Function<Line, BigDecimal> tax) {
		BigDecimal onExits = sum(exits, tax);
		BigDecimal onEntries = sum(entries, tax);
		return new Totals(onExits, onEntries, onExits.subtract(onEntries));
	}

	private static List<Line> withIcms(List<Line> book) {
		return book.stream().filter(line -> line.icmsValue().signum() != 0).toList();
	}

	private static BigDecimal sum(List<Line> book, Function<Line, BigDecimal> value) {
		return book.stream().map(value).reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	/** An NFe the company issued is an entry when its CFOP starts with 1, 2 or 3, an exit otherwise. */
	private static Line toLine(NfeDocument nfe, ZoneId zone) {
		String cfop = nfe.getCfop().code();
		Flow flow = switch (cfop.charAt(0)) {
			case '1', '2', '3' -> Flow.ENTRY;
			default -> Flow.EXIT;
		};
		Map<TaxType, BigDecimal> taxes = nfe.getTaxTotals().byTaxType();
		return new Line(flow, nfe.getAuthorizedAt().atZone(zone).toLocalDate(), nfe.getDocumentSeries(),
				nfe.getDocumentNumber() == null ? null : String.valueOf(nfe.getDocumentNumber()), nfe.getAccessKey(),
				nfe.getRecipient().name(), nfe.getRecipient().document().number(), cfop, nfe.getDocumentTotal(),
				tax(taxes, TaxType.ICMS), tax(taxes, TaxType.IPI), tax(taxes, TaxType.PIS),
				tax(taxes, TaxType.COFINS));
	}

	private static Line toLine(InboundNfe nfe, ZoneId zone) {
		String cfops = nfe.getItems().stream().map(InboundNfeItem::cfop)
				.filter(cfop -> cfop != null && !cfop.isBlank()).distinct().sorted().collect(Collectors.joining("/"));
		InboundNfeTotals totals = nfe.getTotals();
		LocalDate day = nfe.getIssuedAt().atZone(zone).toLocalDate();
		return new Line(Flow.ENTRY, day, nfe.getSeries(), nfe.getNumber(), nfe.getAccessKey(), nfe.getSupplierName(),
				nfe.getSupplierDocument().number(), cfops.isEmpty() ? null : cfops, totals.totalValue(),
				totals.icmsValue(), totals.ipiValue(), totals.pisValue(), totals.cofinsValue());
	}

	private VoidedRange toRange(VoidedNumberRange range) {
		return new VoidedRange(range.getSeries(), range.getStartNumber(), range.getEndNumber(),
				range.getJustification(), range.getSefazProtocol(), range.getVoidedAt());
	}

	private static BigDecimal tax(Map<TaxType, BigDecimal> taxes, TaxType type) {
		return taxes.getOrDefault(type, BigDecimal.ZERO);
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
