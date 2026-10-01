package br.gravita.adapters.inbound.controllers.tax.dtos;

import br.gravita.core.ports.inbound.tax.LivrosFiscaisBooks;
import br.gravita.core.ports.inbound.tax.LivrosFiscaisBooks.Book;
import br.gravita.core.ports.inbound.tax.LivrosFiscaisBooks.IcmsAssessment;
import br.gravita.core.ports.inbound.tax.LivrosFiscaisBooks.TaxSummary;
import br.gravita.core.ports.inbound.tax.LivrosFiscaisReport;
import java.time.YearMonth;
import java.util.UUID;

/** The books and the tax summary as structured data, and the same books rendered: {@code pdf} and {@code txt} are base64. */
public record LivrosFiscaisResponse(UUID companyId, YearMonth period, Book entryBook, Book exitBook,
		IcmsAssessment icmsAssessmentBook, TaxSummary taxSummary, byte[] pdf, byte[] txt) {

	public static LivrosFiscaisResponse from(LivrosFiscaisReport report) {
		LivrosFiscaisBooks books = report.books();
		return new LivrosFiscaisResponse(books.companyId().value(), books.period(), books.entryBook(),
				books.exitBook(), books.icmsAssessmentBook(), books.taxSummary(), report.pdf(), report.txt());
	}
}
