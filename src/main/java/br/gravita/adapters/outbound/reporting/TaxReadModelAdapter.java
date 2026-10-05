package br.gravita.adapters.outbound.reporting;

import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.sales.SalesInvoiceStatus;
import br.gravita.core.domain.sales.SalesOrder;
import br.gravita.core.domain.tax.InboundNfe;
import br.gravita.core.domain.tax.InboundNfeItem;
import br.gravita.core.domain.tax.NfceSale;
import br.gravita.core.domain.tax.NfeDocument;
import br.gravita.core.domain.tax.NfseDocument;
import br.gravita.core.domain.tax.TaxType;
import br.gravita.core.ports.outbound.persistence.sales.SalesInvoiceRepositoryPort;
import br.gravita.core.ports.outbound.persistence.sales.SalesOrderRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.InboundNfeRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.NfceRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.NfeRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.NfseRepositoryPort;
import br.gravita.core.ports.outbound.reporting.TaxReadModelPort;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sales orders carry no company, so {@code companyId} cannot narrow the invoiced total yet. Neither can the fiscal
 * documents be narrowed: the NFC-e keeps no company of its own, so the books list every document of the period. A
 * document's day is the one it falls on in the server's time zone. The NFC-e stores no tax breakdown, so it adds
 * nothing to the assessed taxes.
 */
@PersistenceAdapter
class TaxReadModelAdapter implements TaxReadModelPort {

	static final String NFE = "NFE";
	static final String NFCE = "NFCE";
	static final String NFSE = "NFSE";

	private final SalesOrderRepositoryPort salesOrderRepositoryPort;
	private final SalesInvoiceRepositoryPort salesInvoiceRepositoryPort;
	private final NfeRepositoryPort nfeRepositoryPort;
	private final NfceRepositoryPort nfceRepositoryPort;
	private final InboundNfeRepositoryPort inboundNfeRepositoryPort;
	private final NfseRepositoryPort nfseRepositoryPort;
	private final ZoneId zone = ZoneId.systemDefault();

	TaxReadModelAdapter(final SalesOrderRepositoryPort salesOrderRepositoryPort,
			final SalesInvoiceRepositoryPort salesInvoiceRepositoryPort, final NfeRepositoryPort nfeRepositoryPort,
			final NfceRepositoryPort nfceRepositoryPort, final InboundNfeRepositoryPort inboundNfeRepositoryPort,
			final NfseRepositoryPort nfseRepositoryPort) {
		this.salesOrderRepositoryPort = salesOrderRepositoryPort;
		this.salesInvoiceRepositoryPort = salesInvoiceRepositoryPort;
		this.nfeRepositoryPort = nfeRepositoryPort;
		this.nfceRepositoryPort = nfceRepositoryPort;
		this.inboundNfeRepositoryPort = inboundNfeRepositoryPort;
		this.nfseRepositoryPort = nfseRepositoryPort;
	}

	@Override
	@Transactional(readOnly = true)
	public BigDecimal invoicedTotal(final LocalDate from, final LocalDate to, final UUID companyId) {
		return salesOrderRepositoryPort.findInvoicedByPeriod(from, to).stream()
				.filter(order -> hasIssuedFiscalDocument(order))
				.map(SalesOrder::totalValue).reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	@Override
	@Transactional(readOnly = true)
	public List<FiscalDocumentRecord> entryDocuments(final LocalDate from, final LocalDate to) {
		return inboundNfeRepositoryPort.findIssuedBetween(startOf(from), startOfDayAfter(to)).stream()
				.map(this::toRecord).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<FiscalDocumentRecord> exitDocuments(final LocalDate from, final LocalDate to) {
		final Instant start = startOf(from);
		final Instant end = startOfDayAfter(to);
		final List<FiscalDocumentRecord> exits = new ArrayList<>();
		nfeRepositoryPort.findAuthorizedBetween(start, end).forEach(nfe -> exits.add(toRecord(nfe)));
		nfceRepositoryPort.findAuthorizedBetween(start, end).forEach(nfce -> exits.add(toRecord(nfce)));
		return exits;
	}

	@Override
	@Transactional(readOnly = true)
	public List<DocumentTaxRecord> authorizedDocumentTaxes(final LocalDate from, final LocalDate to) {
		final Instant start = startOf(from);
		final Instant end = startOfDayAfter(to);
		final List<DocumentTaxRecord> taxes = new ArrayList<>();
		nfeRepositoryPort.findAuthorizedBetween(start, end).forEach(nfe -> taxes.add(toTaxRecord(nfe)));
		nfseRepositoryPort.findAuthorizedBetween(start, end).forEach(nfse -> taxes.add(toTaxRecord(nfse)));
		return taxes;
	}

	private DocumentTaxRecord toTaxRecord(final NfeDocument nfe) {
		final Map<TaxType, BigDecimal> byTaxType = nfe.getTaxTotals().byTaxType();
		return new DocumentTaxRecord(NFE, day(nfe.getAuthorizedAt()), amount(byTaxType, TaxType.ICMS),
				amount(byTaxType, TaxType.IPI), amount(byTaxType, TaxType.PIS), amount(byTaxType, TaxType.COFINS),
				BigDecimal.ZERO);
	}

	private DocumentTaxRecord toTaxRecord(final NfseDocument nfse) {
		return new DocumentTaxRecord(NFSE, day(nfse.getAuthorizedAt()), BigDecimal.ZERO, BigDecimal.ZERO,
				BigDecimal.ZERO, BigDecimal.ZERO, nfse.getIssAmount());
	}

	private static BigDecimal amount(final Map<TaxType, BigDecimal> byTaxType, final TaxType type) {
		return byTaxType.getOrDefault(type, BigDecimal.ZERO);
	}

	private FiscalDocumentRecord toRecord(final InboundNfe nfe) {
		final String cfops = nfe.getItems().stream().map(InboundNfeItem::cfop).filter(cfop -> cfop != null && !cfop.isBlank())
				.distinct().sorted().collect(Collectors.joining("/"));
		return new FiscalDocumentRecord(NFE, day(nfe.getIssuedAt()), nfe.getSeries(), nfe.getNumber(),
				nfe.getAccessKey(), nfe.getSupplierName(), nfe.getSupplierDocument().number(),
				cfops.isEmpty() ? null : cfops, nfe.getTotals().totalValue(), nfe.getTotals().icmsValue());
	}

	private FiscalDocumentRecord toRecord(final NfeDocument nfe) {
		final BigDecimal icms = nfe.getTaxTotals().byTaxType().get(TaxType.ICMS);
		return new FiscalDocumentRecord(NFE, day(nfe.getAuthorizedAt()), nfe.getDocumentSeries(),
				nfe.getDocumentNumber() == null ? null : String.valueOf(nfe.getDocumentNumber()), nfe.getAccessKey(),
				nfe.getRecipient().name(), nfe.getRecipient().document().number(), nfe.getCfop().code(),
				nfe.getDocumentTotal(), icms);
	}

	/** An NFC-e stores no tax breakdown, so it states no ICMS here; its sale total is still booked. */
	private FiscalDocumentRecord toRecord(final NfceSale sale) {
		return new FiscalDocumentRecord(NFCE, day(sale.getCreatedAt()), sale.getDocumentSeries(),
				sale.getDocumentNumber() == null ? null : String.valueOf(sale.getDocumentNumber()),
				sale.getAccessKey(), null, sale.getCustomerCpf(), null, sale.getSaleTotal(), null);
	}

	private LocalDate day(final Instant instant) {
		return instant.atZone(zone).toLocalDate();
	}

	private Instant startOf(final LocalDate day) {
		return day.atStartOfDay(zone).toInstant();
	}

	private Instant startOfDayAfter(final LocalDate day) {
		return day.plusDays(1).atStartOfDay(zone).toInstant();
	}

	private boolean hasIssuedFiscalDocument(final SalesOrder order) {
		return salesInvoiceRepositoryPort.findByOrderId(order.getId())
				.filter(invoice -> invoice.getStatus() == SalesInvoiceStatus.ISSUED).isPresent();
	}
}
