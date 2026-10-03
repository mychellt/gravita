package br.gravita.core.ports.messaging.records;

import br.gravita.core.ports.messaging.SendFiscalDocumentByEmailPort;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * UC-M7-02: enough to let the downstream WhatsApp sender (outside this repo,
 * same as {@link SendFiscalDocumentByEmailPort}'s downstream mail sender)
 * build the actual message - the customer's registered WhatsApp number plus
 * the quote's own summary.
 */
public record SendQuoteByWhatsAppRequest(String phoneNumber, UUID quoteId, BigDecimal totalValue,
                                         LocalDate validUntil) {

    public SendQuoteByWhatsAppRequest {
        Objects.requireNonNull(phoneNumber, "phoneNumber");
        Objects.requireNonNull(quoteId, "quoteId");
        Objects.requireNonNull(totalValue, "totalValue");
        Objects.requireNonNull(validUntil, "validUntil");
    }
}
