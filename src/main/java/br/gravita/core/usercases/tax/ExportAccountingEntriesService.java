package br.gravita.core.usercases.tax;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.masterdata.Company;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.tax.InboundNfe;
import br.gravita.core.domain.tax.InboundNfeItem;
import br.gravita.core.domain.tax.InboundNfeTotals;
import br.gravita.core.domain.tax.NfeDocument;
import br.gravita.core.domain.tax.TaxType;
import br.gravita.core.ports.inbound.tax.AccountingEntry;
import br.gravita.core.ports.inbound.tax.AccountingEntry.Flow;
import br.gravita.core.ports.inbound.tax.AccountingExportFile;
import br.gravita.core.ports.inbound.tax.ExportAccountingEntriesCommand;
import br.gravita.core.ports.inbound.tax.ExportAccountingEntriesUseCase;
import br.gravita.core.ports.outbound.persistence.CompanyRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.InboundNfeRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.NfeRepositoryPort;
import br.gravita.core.ports.outbound.tax.ExportAccountingFilePort;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Exports a company's month of fiscal documents as accounting entries, as CSV or TXT (UC-M2-14). Nothing is written
 * but the file: the entries are derived from the documents {@code tax} already holds.
 *
 * <p>Every NFe the company issued that SEFAZ authorized in the month (by the day of authorization; cancelled and
 * rejected NFe are not exported) and every NFe it received whose receipt was confirmed (by the day its supplier
 * issued it) is one entry, oldest first, and an entry's flow follows the CFOP rules of the fiscal books: an issued
 * NFe under a CFOP starting with 1, 2 or 3 is an entry, any other an exit, and a received NFe is always an entry. A
 * document's day is the one it falls on in the clock's time zone. How the entries are laid out is the file port's
 * concern, not this service's.
 */
@UseCase
public class ExportAccountingEntriesService implements ExportAccountingEntriesUseCase {

	private static final Comparator<AccountingEntry> ENTRY_ORDER = Comparator.comparing(AccountingEntry::date)
			.thenComparing(AccountingEntry::accessKey, Comparator.nullsFirst(Comparator.naturalOrder()));

	private final CompanyRepositoryPort companyRepositoryPort;
	private final NfeRepositoryPort nfeRepositoryPort;
	private final InboundNfeRepositoryPort inboundNfeRepositoryPort;
	private final ExportAccountingFilePort exportAccountingFilePort;
	private final Clock clock;

	@Autowired
	public ExportAccountingEntriesService(CompanyRepositoryPort companyRepositoryPort,
			NfeRepositoryPort nfeRepositoryPort, InboundNfeRepositoryPort inboundNfeRepositoryPort,
			ExportAccountingFilePort exportAccountingFilePort) {
		this(companyRepositoryPort, nfeRepositoryPort, inboundNfeRepositoryPort, exportAccountingFilePort,
				Clock.systemDefaultZone());
	}

	public ExportAccountingEntriesService(CompanyRepositoryPort companyRepositoryPort,
			NfeRepositoryPort nfeRepositoryPort, InboundNfeRepositoryPort inboundNfeRepositoryPort,
			ExportAccountingFilePort exportAccountingFilePort, Clock clock) {
		this.companyRepositoryPort = companyRepositoryPort;
		this.nfeRepositoryPort = nfeRepositoryPort;
		this.inboundNfeRepositoryPort = inboundNfeRepositoryPort;
		this.exportAccountingFilePort = exportAccountingFilePort;
		this.clock = clock;
	}

	@Override
	public AccountingExportFile execute(ExportAccountingEntriesCommand command) {
		CompanyId companyId = command.companyId();
		Company company = companyRepositoryPort.findById(companyId)
				.orElseThrow(() -> new ResourceNotFoundException("Company not found: " + companyId.value()));

		YearMonth period = command.period();
		ZoneId zone = clock.getZone();
		Instant from = period.atDay(1).atStartOfDay(zone).toInstant();
		Instant to = period.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant();

		List<AccountingEntry> entries = new ArrayList<>();
		nfeRepositoryPort.findAuthorizedByCompanyBetween(companyId, from, to)
				.forEach(nfe -> entries.add(toEntry(nfe, zone)));
		inboundNfeRepositoryPort.findConfirmedByCompanyBetween(companyId, from, to)
				.forEach(nfe -> entries.add(toEntry(nfe, zone)));
		entries.sort(ENTRY_ORDER);

		byte[] content = exportAccountingFilePort.export(List.copyOf(entries), command.format());
		String fileName = "accounting-entries-" + company.getCnpj().number() + "-" + period + "."
				+ command.format().extension();
		return new AccountingExportFile(fileName, command.format(), content, entries.size());
	}

	private static AccountingEntry toEntry(NfeDocument nfe, ZoneId zone) {
		String cfop = nfe.getCfop().code();
		Flow flow = switch (cfop.charAt(0)) {
			case '1', '2', '3' -> Flow.ENTRY;
			default -> Flow.EXIT;
		};
		Map<TaxType, BigDecimal> taxes = nfe.getTaxTotals().byTaxType();
		return new AccountingEntry(flow, nfe.getAuthorizedAt().atZone(zone).toLocalDate(), nfe.getDocumentSeries(),
				nfe.getDocumentNumber() == null ? null : String.valueOf(nfe.getDocumentNumber()), nfe.getAccessKey(),
				nfe.getRecipient().name(), nfe.getRecipient().document().number(), cfop, nfe.getDocumentTotal(),
				tax(taxes, TaxType.ICMS), tax(taxes, TaxType.IPI), tax(taxes, TaxType.PIS),
				tax(taxes, TaxType.COFINS));
	}

	private static AccountingEntry toEntry(InboundNfe nfe, ZoneId zone) {
		String cfops = nfe.getItems().stream().map(InboundNfeItem::cfop)
				.filter(cfop -> cfop != null && !cfop.isBlank()).distinct().sorted().collect(Collectors.joining("/"));
		InboundNfeTotals totals = nfe.getTotals();
		return new AccountingEntry(Flow.ENTRY, nfe.getIssuedAt().atZone(zone).toLocalDate(), nfe.getSeries(),
				nfe.getNumber(), nfe.getAccessKey(), nfe.getSupplierName(), nfe.getSupplierDocument().number(),
				cfops.isEmpty() ? null : cfops, totals.totalValue(), totals.icmsValue(), totals.ipiValue(),
				totals.pisValue(), totals.cofinsValue());
	}

	private static BigDecimal tax(Map<TaxType, BigDecimal> taxes, TaxType type) {
		return taxes.getOrDefault(type, BigDecimal.ZERO);
	}
}
