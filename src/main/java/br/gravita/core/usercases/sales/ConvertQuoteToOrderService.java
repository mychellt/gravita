package br.gravita.core.usercases.sales;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.sales.Quote;
import br.gravita.core.domain.sales.QuoteId;
import br.gravita.core.domain.sales.QuoteNotFoundException;
import br.gravita.core.domain.sales.SalesOrder;
import br.gravita.core.domain.sales.SalesOrderId;
import br.gravita.core.domain.sales.SalesOrderItem;
import br.gravita.core.ports.inbound.sales.ConvertQuoteToOrderCommand;
import br.gravita.core.ports.inbound.sales.ConvertQuoteToOrderUseCase;
import br.gravita.core.ports.inbound.sales.SalesOrderView;
import br.gravita.core.ports.outbound.persistence.sales.QuoteRepositoryPort;
import br.gravita.core.ports.outbound.persistence.sales.SalesOrderRepositoryPort;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@UseCase
public class ConvertQuoteToOrderService implements ConvertQuoteToOrderUseCase {

	private final QuoteRepositoryPort quoteRepositoryPort;
	private final SalesOrderRepositoryPort salesOrderRepositoryPort;

	@Override
	public SalesOrderView execute(ConvertQuoteToOrderCommand command) {
		Quote quote = quoteRepositoryPort.findById(QuoteId.of(command.quoteId()))
				.orElseThrow(() -> new QuoteNotFoundException(command.quoteId()));

		Quote convertedQuote = quote.convert(LocalDate.now());

		List<SalesOrderItem> items = quote.getItems().stream().map(SalesOrderItem::fromQuoteItem).toList();
		SalesOrder order = SalesOrder.createFromQuote(SalesOrderId.of(UUID.randomUUID()), quote.getId(),
				quote.getCustomerId(), items);

		SalesOrder saved = salesOrderRepositoryPort.save(order);
		quoteRepositoryPort.save(convertedQuote);

		return SalesOrderView.from(saved);
	}
}
