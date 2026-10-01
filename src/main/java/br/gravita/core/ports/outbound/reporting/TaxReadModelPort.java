package br.gravita.core.ports.outbound.reporting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TaxReadModelPort {

	/** The total of the orders invoiced over {@code [from, to]} (inclusive) that have an issued fiscal document. */
	BigDecimal invoicedTotal(LocalDate from, LocalDate to, UUID companyId);

	/**
	 * The documents received over {@code [from, to]} (inclusive), by the day each was issued by its supplier, in no
	 * particular order.
	 */
	List<FiscalDocumentRecord> entryDocuments(LocalDate from, LocalDate to);

	/**
	 * The NFe and NFC-e authorized over {@code [from, to]} (inclusive), by the day each was authorized, in no
	 * particular order. Documents that were rejected, cancelled or voided are not authorized and are absent.
	 */
	List<FiscalDocumentRecord> exitDocuments(LocalDate from, LocalDate to);

	/**
	 * The tax each NFe and NFSe authorized over {@code [from, to]} (inclusive) states, by the day each was authorized,
	 * in no particular order. Documents that were rejected, cancelled or voided are not authorized and are absent.
	 */
	List<DocumentTaxRecord> authorizedDocumentTaxes(LocalDate from, LocalDate to);

	/**
	 * A fiscal document as the books list it. {@code documentModel} is {@code NFE} or {@code NFCE}. A {@code null}
	 * {@code cfop}, counterpart or ICMS means the source document does not carry it; an absent ICMS counts as zero.
	 */
	record FiscalDocumentRecord(String documentModel, LocalDate date, String series, String number, String accessKey,
			String counterpartName, String counterpartDocument, String cfop, BigDecimal totalValue,
			BigDecimal icmsValue) {
	}

	/**
	 * The tax one authorized document states. {@code documentModel} is {@code NFE} or {@code NFSE}. A tax the
	 * document does not state is zero, never {@code null}: an NFe carries ICMS, IPI, PIS and COFINS, an NFSe only ISS.
	 */
	record DocumentTaxRecord(String documentModel, LocalDate date, BigDecimal icms, BigDecimal ipi, BigDecimal pis,
			BigDecimal cofins, BigDecimal iss) {
	}
}
