package br.gravita.core.usercases.sales;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.ContactDomain;
import br.gravita.core.domain.ContactType;
import br.gravita.core.domain.CustomerDomain;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.sales.Quote;
import br.gravita.core.domain.sales.QuoteDeliveryChannel;
import br.gravita.core.domain.sales.QuoteId;
import br.gravita.core.domain.sales.QuoteNotFoundException;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.sales.SendQuoteCommand;
import br.gravita.core.ports.inbound.sales.SendQuoteUseCase;
import br.gravita.core.ports.messaging.SendQuoteByWhatsAppPort;
import br.gravita.core.ports.messaging.records.SendQuoteByWhatsAppRequest;
import br.gravita.core.ports.outbound.persistence.CustomerRepositoryPort;
import br.gravita.core.ports.outbound.persistence.sales.QuoteRepositoryPort;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * UC-M7-02. The expiry check runs up front - via {@link Quote#send} - before
 * any channel-specific delivery work, same as {@code CancelNfeService}
 * rejects before calling SEFAZ. {@code PDF} delivery has no dedicated
 * outbound port (it reuses a shared document-rendering capability, per the
 * use case spec's notes), so only {@code WHATSAPP} drives
 * {@link SendQuoteByWhatsAppPort}. The quote is only persisted as
 * {@code SENT} once delivery actually succeeds.
 */
@RequiredArgsConstructor
@UseCase
public class SendQuoteService implements SendQuoteUseCase {

    private final QuoteRepositoryPort quoteRepositoryPort;
    private final CustomerRepositoryPort customerRepositoryPort;
    private final SendQuoteByWhatsAppPort sendQuoteByWhatsAppPort;

    @Override
    public void execute(SendQuoteCommand command) {
        Quote quote = quoteRepositoryPort.findById(QuoteId.of(command.quoteId()))
                .orElseThrow(() -> new QuoteNotFoundException(command.quoteId()));

        Quote sent = quote.send(LocalDate.now());

        if (command.channel() == QuoteDeliveryChannel.WHATSAPP) {
            String phoneNumber = resolveWhatsAppContact(quote.getCustomerId());
            sendQuoteByWhatsAppPort.send(new SendQuoteByWhatsAppRequest(phoneNumber, quote.getId().value(),
                    quote.totalValue(), quote.getValidUntil()));
        }

        quoteRepositoryPort.save(sent);
    }

    private String resolveWhatsAppContact(UUID customerId) {
        CustomerDomain customer = customerRepositoryPort.get(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + customerId));
        List<ContactDomain> contacts = customer.getContacts();
        return (contacts == null ? List.<ContactDomain>of() : contacts).stream()
                .filter(contact -> contact.getType() == ContactType.WHATSAPP)
                .map(ContactDomain::getValue)
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException(
                        "Customer " + customerId + " has no registered WhatsApp contact"));
    }
}
