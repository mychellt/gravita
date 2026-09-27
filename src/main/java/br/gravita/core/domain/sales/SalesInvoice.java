package br.gravita.core.domain.sales;

import br.gravita.core.domain.shared.BusinessRuleException;
import java.util.List;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Links a {@link SalesOrder} to every fiscal document actually issued for it
 * (UC-M7-06). A product/service line-item split can produce one document per
 * nature, so {@code fiscalDocuments} is a list rather than a single ref.
 */
@AllArgsConstructor
@Getter
public final class SalesInvoice {

	private final SalesInvoiceId id;
	private final SalesOrderId orderId;
	private final List<FiscalDocumentRef> fiscalDocuments;
	private final SalesInvoiceStatus status;

	public static SalesInvoice issue(SalesInvoiceId id, SalesOrderId orderId, List<FiscalDocumentRef> fiscalDocuments) {
		Objects.requireNonNull(id, "id is required");
		Objects.requireNonNull(orderId, "orderId is required");
		return new SalesInvoice(id, orderId, requireNonEmptyDocuments(fiscalDocuments), SalesInvoiceStatus.ISSUED);
	}

	public static SalesInvoice of(SalesInvoiceId id, SalesOrderId orderId, List<FiscalDocumentRef> fiscalDocuments,
			SalesInvoiceStatus status) {
		return new SalesInvoice(id, orderId, fiscalDocuments == null ? List.of() : List.copyOf(fiscalDocuments),
				status);
	}

	private static List<FiscalDocumentRef> requireNonEmptyDocuments(List<FiscalDocumentRef> fiscalDocuments) {
		List<FiscalDocumentRef> copy = fiscalDocuments == null ? List.of() : List.copyOf(fiscalDocuments);
		if (copy.isEmpty()) {
			throw new BusinessRuleException("A sales invoice must link at least one issued fiscal document");
		}
		return copy;
	}
}
