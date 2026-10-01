package br.gravita.core.ports.inbound.reporting;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One line of a fiscal book: a document booked in the period. {@code date} is the day the document was authorized
 * (exits) or issued by the supplier (entries). {@code counterpartName} and {@code counterpartDocument} are the
 * supplier of an entry and the recipient of an exit; an NFC-e sale to an anonymous consumer has neither. {@code cfop}
 * is {@code null} where the source document does not carry one (NFC-e) and lists every distinct CFOP, separated by
 * {@code /}, when an entry mixes several. {@code icmsValue} is the ICMS stated on the document: a credit on an
 * entry, a debit on an exit. It is a read model projected from {@code tax}.
 */
public record FiscalBookEntry(FiscalBookFlow flow, LocalDate date, String documentModel, String series, String number,
		String accessKey, String counterpartName, String counterpartDocument, String cfop, BigDecimal totalValue,
		BigDecimal icmsValue) {
}
