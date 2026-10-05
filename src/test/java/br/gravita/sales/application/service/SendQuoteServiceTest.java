package br.gravita.sales.application.service;

import br.gravita.core.domain.ContactDomain;
import br.gravita.core.domain.ContactType;
import br.gravita.core.domain.CustomerDomain;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.sales.*;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.sales.SendQuoteCommand;
import br.gravita.core.ports.messaging.SendQuoteByWhatsAppPort;
import br.gravita.core.ports.messaging.records.SendQuoteByWhatsAppRequest;
import br.gravita.core.ports.outbound.persistence.CustomerRepositoryPort;
import br.gravita.core.ports.outbound.persistence.sales.QuoteRepositoryPort;
import br.gravita.core.usercases.sales.SendQuoteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SendQuoteServiceTest {

    @Mock
    private QuoteRepositoryPort quoteRepositoryPort;

    @Mock
    private CustomerRepositoryPort customerRepositoryPort;

    @Mock
    private SendQuoteByWhatsAppPort sendQuoteByWhatsAppPort;

    @InjectMocks
    private SendQuoteService service;

    private UUID customerId;
    private QuoteId quoteId;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        quoteId = QuoteId.of(UUID.randomUUID());
        lenient().when(quoteRepositoryPort.save(any(Quote.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("Sending as PDF moves the quote to SENT without calling WhatsApp")
    void sendingAsPdfMovesTheQuoteToSentWithoutCallingWhatsApp() {
        final Quote quote = draftQuote(LocalDate.now().plusDays(5));
        when(quoteRepositoryPort.findById(quoteId)).thenReturn(Optional.of(quote));

        service.execute(new SendQuoteCommand(quoteId.value(), QuoteDeliveryChannel.PDF));

        final ArgumentCaptor<Quote> saved = ArgumentCaptor.forClass(Quote.class);
        verify(quoteRepositoryPort).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(QuoteStatus.SENT);
        verify(sendQuoteByWhatsAppPort, never()).send(any());
    }

    @Test
    @DisplayName("Sending by WhatsApp uses the customer's registered WhatsApp contact")
    void sendingByWhatsAppUsesTheCustomersRegisteredWhatsAppContact() {
        final Quote quote = draftQuote(LocalDate.now().plusDays(5));
        when(quoteRepositoryPort.findById(quoteId)).thenReturn(Optional.of(quote));
        when(customerRepositoryPort.get(customerId)).thenReturn(Optional.of(CustomerDomain.builder()
                .contacts(List.of(new ContactDomain(ContactType.EMAIL, "cliente@example.com"),
                        new ContactDomain(ContactType.WHATSAPP, "+5511999998888")))
                .build()));

        service.execute(new SendQuoteCommand(quoteId.value(), QuoteDeliveryChannel.WHATSAPP));

        final ArgumentCaptor<SendQuoteByWhatsAppRequest> captor = ArgumentCaptor.forClass(SendQuoteByWhatsAppRequest.class);
        verify(sendQuoteByWhatsAppPort).send(captor.capture());
        assertThat(captor.getValue().phoneNumber()).isEqualTo("+5511999998888");
        assertThat(captor.getValue().quoteId()).isEqualTo(quoteId.value());

        final ArgumentCaptor<Quote> saved = ArgumentCaptor.forClass(Quote.class);
        verify(quoteRepositoryPort).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(QuoteStatus.SENT);
    }

    @Test
    @DisplayName("Rejects sending an expired quote before any delivery attempt")
    void rejectsSendingAnExpiredQuoteBeforeAnyDeliveryAttempt() {
        final Quote quote = draftQuote(LocalDate.now().minusDays(1));
        when(quoteRepositoryPort.findById(quoteId)).thenReturn(Optional.of(quote));

        assertThatThrownBy(() -> service.execute(new SendQuoteCommand(quoteId.value(), QuoteDeliveryChannel.PDF)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("has expired");

        verify(sendQuoteByWhatsAppPort, never()).send(any());
        verify(quoteRepositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("A customer without a WhatsApp contact is rejected rather than silently skipped")
    void customerWithNoWhatsAppContactOnFileIsRejectedRatherThanSilentlySkipped() {
        final Quote quote = draftQuote(LocalDate.now().plusDays(5));
        when(quoteRepositoryPort.findById(quoteId)).thenReturn(Optional.of(quote));
        when(customerRepositoryPort.get(customerId)).thenReturn(Optional.of(CustomerDomain.builder()
                .contacts(List.of(new ContactDomain(ContactType.EMAIL, "cliente@example.com")))
                .build()));

        assertThatThrownBy(
                () -> service.execute(new SendQuoteCommand(quoteId.value(), QuoteDeliveryChannel.WHATSAPP)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("WhatsApp");

        verify(sendQuoteByWhatsAppPort, never()).send(any());
        verify(quoteRepositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("Sending a quote that does not exist is rejected")
    void quoteThatDoesNotExistIsRejected() {
        when(quoteRepositoryPort.findById(quoteId)).thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> service.execute(new SendQuoteCommand(quoteId.value(), QuoteDeliveryChannel.PDF)))
                .isInstanceOf(QuoteNotFoundException.class);
    }

    @Test
    @DisplayName("Sending by WhatsApp for a customer that does not exist is rejected")
    void whatsAppCustomerThatDoesNotExistIsRejected() {
        final Quote quote = draftQuote(LocalDate.now().plusDays(5));
        when(quoteRepositoryPort.findById(quoteId)).thenReturn(Optional.of(quote));
        when(customerRepositoryPort.get(customerId)).thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> service.execute(new SendQuoteCommand(quoteId.value(), QuoteDeliveryChannel.WHATSAPP)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(quoteRepositoryPort, never()).save(any());
    }

    private Quote draftQuote(final LocalDate validUntil) {
        final QuoteItem item = new QuoteItem(UUID.randomUUID(), BigDecimal.ONE, new BigDecimal("100.00"), BigDecimal.ZERO);
        return Quote.of(quoteId, customerId, UUID.randomUUID(), List.of(item), validUntil, QuoteStatus.DRAFT);
    }
}
