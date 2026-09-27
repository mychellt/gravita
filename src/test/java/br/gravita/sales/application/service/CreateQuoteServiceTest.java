package br.gravita.sales.application.service;

import br.gravita.core.domain.sales.Quote;
import br.gravita.core.domain.sales.QuoteItem;
import br.gravita.core.domain.sales.QuoteStatus;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.sales.CreateQuoteCommand;
import br.gravita.core.ports.inbound.sales.QuoteView;
import br.gravita.core.ports.outbound.persistence.sales.QuoteRepositoryPort;
import br.gravita.core.usercases.sales.CreateQuoteService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateQuoteServiceTest {

	@Mock
	private QuoteRepositoryPort quoteRepositoryPort;

	@InjectMocks
	private CreateQuoteService service;

	@Test
	void persistsADraftQuoteWithTheSubmittedItemsAndValidity() {
		when(quoteRepositoryPort.save(any(Quote.class))).thenAnswer(invocation -> invocation.getArgument(0));
		UUID customerId = UUID.randomUUID();
		LocalDate validUntil = LocalDate.now().plusDays(10);
		List<QuoteItem> items = List.of(
				new QuoteItem(UUID.randomUUID(), new BigDecimal("2"), new BigDecimal("19.90"), new BigDecimal("3.80")),
				new QuoteItem(UUID.randomUUID(), BigDecimal.ONE, new BigDecimal("150.00"), BigDecimal.ZERO));

		UUID salespersonId = UUID.randomUUID();

		QuoteView view = service.execute(new CreateQuoteCommand(customerId, salespersonId, items, validUntil));

		ArgumentCaptor<Quote> saved = ArgumentCaptor.forClass(Quote.class);
		verify(quoteRepositoryPort).save(saved.capture());
		assertThat(saved.getValue().getId()).isNotNull();
		assertThat(saved.getValue().getCustomerId()).isEqualTo(customerId);
		assertThat(saved.getValue().getStatus()).isEqualTo(QuoteStatus.DRAFT);
		assertThat(saved.getValue().getValidUntil()).isEqualTo(validUntil);
		assertThat(saved.getValue().getItems()).containsExactlyElementsOf(items);

		assertThat(view.id()).isEqualTo(saved.getValue().getId().value());
		assertThat(view.status()).isEqualTo(QuoteStatus.DRAFT);
		assertThat(view.customerId()).isEqualTo(customerId);
		assertThat(view.validUntil()).isEqualTo(validUntil);
		assertThat(view.items()).containsExactlyElementsOf(items);
		assertThat(view.totalValue()).isEqualByComparingTo("186.00");
	}

	@Test
	void rejectsAQuoteWithNoItemsWithoutPersistingIt() {
		assertThatThrownBy(() -> service.execute(
				new CreateQuoteCommand(UUID.randomUUID(), UUID.randomUUID(), List.of(), LocalDate.now().plusDays(1))))
				.isInstanceOf(BusinessRuleException.class);

		verify(quoteRepositoryPort, never()).save(any());
	}

	@Test
	void rejectsAValidityDateThatIsNotInTheFutureWithoutPersistingIt() {
		List<QuoteItem> items = List.of(new QuoteItem(UUID.randomUUID(), BigDecimal.ONE, BigDecimal.TEN, null));

		assertThatThrownBy(() -> service.execute(
				new CreateQuoteCommand(UUID.randomUUID(), UUID.randomUUID(), items, LocalDate.now())))
				.isInstanceOf(BusinessRuleException.class);

		verify(quoteRepositoryPort, never()).save(any());
	}
}
