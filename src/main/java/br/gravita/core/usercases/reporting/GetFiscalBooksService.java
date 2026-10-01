package br.gravita.core.usercases.reporting;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.exceptions.ForbiddenException;
import br.gravita.core.ports.inbound.reporting.FiscalBookEntry;
import br.gravita.core.ports.inbound.reporting.FiscalBookFlow;
import br.gravita.core.ports.inbound.reporting.FiscalBooks;
import br.gravita.core.ports.inbound.reporting.FiscalBooksQuery;
import br.gravita.core.ports.inbound.reporting.GetFiscalBooksUseCase;
import br.gravita.core.ports.outbound.reporting.PermissionCheckPort;
import br.gravita.core.ports.outbound.reporting.RenderPdfPort;
import br.gravita.core.ports.outbound.reporting.TaxReadModelPort;
import br.gravita.core.ports.outbound.reporting.TaxReadModelPort.FiscalDocumentRecord;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Builds the Entries, Exits and ICMS Assessment books of a month (doc §10.2) from the fiscal documents {@code tax}
 * holds, and renders them as PDF and TXT. The entries are the received NFe by their issue day and the exits the
 * authorized NFe and NFC-e by their authorization day, so every book line is a document {@code tax} authorized or
 * received. The assessment lists the documents that stated ICMS, the exits' as debits and the entries' as credits;
 * its balance is debits minus credits. Both files are laid out from the same rows, so they cannot disagree.
 */
@UseCase
public class GetFiscalBooksService implements GetFiscalBooksUseCase {

	static final String SCREEN = "fiscal-books";

	private static final Comparator<FiscalBookEntry> DOCUMENT_ORDER = Comparator
			.comparing(FiscalBookEntry::date).thenComparing(FiscalBookEntry::documentModel)
			.thenComparing(FiscalBookEntry::series, Comparator.nullsFirst(Comparator.naturalOrder()))
			.thenComparing(FiscalBookEntry::number, Comparator.nullsFirst(Comparator.naturalOrder()))
			.thenComparing(FiscalBookEntry::accessKey, Comparator.nullsFirst(Comparator.naturalOrder()));

	private final TaxReadModelPort taxReadModelPort;
	private final RenderPdfPort renderPdfPort;
	private final PermissionCheckPort permissionCheckPort;

	public GetFiscalBooksService(TaxReadModelPort taxReadModelPort, RenderPdfPort renderPdfPort,
			PermissionCheckPort permissionCheckPort) {
		this.taxReadModelPort = taxReadModelPort;
		this.renderPdfPort = renderPdfPort;
		this.permissionCheckPort = permissionCheckPort;
	}

	@Override
	public FiscalBooks execute(FiscalBooksQuery query) {
		if (!permissionCheckPort.canView(query.requesterId(), SCREEN)) {
			throw new ForbiddenException("The user's profile cannot view the fiscal books");
		}
		LocalDate from = query.period().atDay(1);
		LocalDate to = query.period().atEndOfMonth();
		List<FiscalBookEntry> entries = book(taxReadModelPort.entryDocuments(from, to), FiscalBookFlow.ENTRY);
		List<FiscalBookEntry> exits = book(taxReadModelPort.exitDocuments(from, to), FiscalBookFlow.EXIT);

		BigDecimal icmsDebit = totalIcms(exits);
		BigDecimal icmsCredit = totalIcms(entries);
		List<FiscalBookEntry> icmsAssessment = new ArrayList<>(withIcms(exits));
		icmsAssessment.addAll(withIcms(entries));

		FiscalBooksLayout layout = new FiscalBooksLayout(query.period(), entries, exits, icmsAssessment, icmsDebit,
				icmsCredit);
		return new FiscalBooks(query.period(), entries, exits, List.copyOf(icmsAssessment), icmsDebit, icmsCredit,
				icmsDebit.subtract(icmsCredit), renderPdfPort.render(layout.pdfReport()),
				layout.txt().getBytes(StandardCharsets.UTF_8));
	}

	private static List<FiscalBookEntry> book(List<FiscalDocumentRecord> documents, FiscalBookFlow flow) {
		return documents.stream().map(document -> toEntry(document, flow)).sorted(DOCUMENT_ORDER).toList();
	}

	private static FiscalBookEntry toEntry(FiscalDocumentRecord document, FiscalBookFlow flow) {
		return new FiscalBookEntry(flow, document.date(), document.documentModel(), document.series(),
				document.number(), document.accessKey(), document.counterpartName(), document.counterpartDocument(),
				document.cfop(), document.totalValue(), document.icmsValue() == null ? BigDecimal.ZERO : document.icmsValue());
	}

	private static List<FiscalBookEntry> withIcms(List<FiscalBookEntry> book) {
		return book.stream().filter(entry -> entry.icmsValue().signum() != 0).toList();
	}

	private static BigDecimal totalIcms(List<FiscalBookEntry> book) {
		return book.stream().map(FiscalBookEntry::icmsValue).reduce(BigDecimal.ZERO, BigDecimal::add);
	}
}
