package br.gravita.sales.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.sales.Quote;
import br.gravita.core.domain.sales.QuoteId;
import br.gravita.core.domain.sales.QuoteItem;
import br.gravita.core.domain.sales.QuoteNotFoundException;
import br.gravita.core.domain.sales.QuoteStatus;
import br.gravita.core.domain.sales.SalesOrder;
import br.gravita.core.domain.sales.SalesOrderStatus;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.sales.ConvertQuoteToOrderCommand;
import br.gravita.core.ports.inbound.sales.SalesOrderView;
import br.gravita.core.ports.outbound.persistence.sales.QuoteRepositoryPort;
import br.gravita.core.ports.outbound.persistence.sales.SalesOrderRepositoryPort;
import br.gravita.core.usercases.sales.ConvertQuoteToOrderService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConvertQuoteToOrderServiceTest {

	@Mock
	private QuoteRepositoryPort quoteRepositoryPort;

	@Mock
	private SalesOrderRepositoryPort salesOrderRepositoryPort;

	@InjectMocks
	private ConvertQuoteToOrderService service;

	@Test
	void convertsADraftQuoteIntoADraftOrderCarryingOverCustomerItemsPricesAndDiscounts() {
		UUID customerId = UUID.randomUUID();
		List<QuoteItem> items = List.of(
				new QuoteItem(UUID.randomUUID(), new BigDecimal("2"), new BigDecimal("19.90"), new BigDecimal("3.80")),
				new QuoteItem(UUID.randomUUID(), BigDecimal.ONE, new BigDecimal("150.00"), BigDecimal.ZERO));
		Quote quote = Quote.create(QuoteId.of(UUID.randomUUID()), customerId, UUID.randomUUID(), items,
				LocalDate.now().plusDays(5), LocalDate.now());
		when(quoteRepositoryPort.findById(quote.getId())).thenReturn(Optional.of(quote));
		when(salesOrderRepositoryPort.save(any(SalesOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

		SalesOrderView view = service.execute(new ConvertQuoteToOrderCommand(quote.getId().value()));

		ArgumentCaptor<SalesOrder> savedOrder = ArgumentCaptor.forClass(SalesOrder.class);
		verify(salesOrderRepositoryPort).save(savedOrder.capture());
		assertThat(savedOrder.getValue().getStatus()).isEqualTo(SalesOrderStatus.DRAFT);
		assertThat(savedOrder.getValue().getOriginQuoteId()).isEqualTo(quote.getId());
		assertThat(savedOrder.getValue().getCustomerId()).isEqualTo(customerId);
		assertThat(savedOrder.getValue().getItems()).hasSize(2);
		assertThat(savedOrder.getValue().getItems().get(0).productOrServiceId())
				.isEqualTo(items.get(0).productOrServiceId());
		assertThat(savedOrder.getValue().getItems().get(0).unitPrice()).isEqualByComparingTo(items.get(0).unitPrice());
		assertThat(savedOrder.getValue().getItems().get(0).discount()).isEqualByComparingTo(items.get(0).discount());

		ArgumentCaptor<Quote> savedQuote = ArgumentCaptor.forClass(Quote.class);
		verify(quoteRepositoryPort).save(savedQuote.capture());
		assertThat(savedQuote.getValue().getStatus()).isEqualTo(QuoteStatus.CONVERTED);

		assertThat(view.status()).isEqualTo(SalesOrderStatus.DRAFT);
		assertThat(view.originQuoteId()).isEqualTo(quote.getId().value());
		assertThat(view.customerId()).isEqualTo(customerId);
	}

	@Test
	void rejectsConvertingAQuoteThatDoesNotExist() {
		UUID quoteId = UUID.randomUUID();
		when(quoteRepositoryPort.findById(any())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(new ConvertQuoteToOrderCommand(quoteId)))
				.isInstanceOf(QuoteNotFoundException.class);

		verify(salesOrderRepositoryPort, never()).save(any());
		verify(quoteRepositoryPort, never()).save(any());
	}

	@Test
	void rejectsConvertingAnAlreadyConvertedQuoteWithoutPersistingAnOrder() {
		Quote convertedQuote = Quote.create(QuoteId.of(UUID.randomUUID()), UUID.randomUUID(), UUID.randomUUID(),
				List.of(new QuoteItem(UUID.randomUUID(), BigDecimal.ONE, BigDecimal.TEN, null)),
				LocalDate.now().plusDays(5), LocalDate.now()).convert(LocalDate.now());
		when(quoteRepositoryPort.findById(convertedQuote.getId())).thenReturn(Optional.of(convertedQuote));

		assertThatThrownBy(() -> service.execute(new ConvertQuoteToOrderCommand(convertedQuote.getId().value())))
				.isInstanceOf(BusinessRuleException.class);

		verify(salesOrderRepositoryPort, never()).save(any());
		verify(quoteRepositoryPort, never()).save(any());
	}

	@Test
	void rejectsConvertingAnExpiredQuoteWithoutPersistingAnOrder() {
		Quote expiredQuote = Quote.of(QuoteId.of(UUID.randomUUID()), UUID.randomUUID(), UUID.randomUUID(),
				List.of(new QuoteItem(UUID.randomUUID(), BigDecimal.ONE, BigDecimal.TEN, null)),
				LocalDate.now().minusDays(1), QuoteStatus.SENT);
		when(quoteRepositoryPort.findById(expiredQuote.getId())).thenReturn(Optional.of(expiredQuote));

		assertThatThrownBy(() -> service.execute(new ConvertQuoteToOrderCommand(expiredQuote.getId().value())))
				.isInstanceOf(BusinessRuleException.class);

		verify(salesOrderRepositoryPort, never()).save(any());
		verify(quoteRepositoryPort, never()).save(any());
	}
}
