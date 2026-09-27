package br.gravita.core.domain.sales;

/**
 * UC-M7-02: how a quote is delivered to the customer. {@code WHATSAPP}
 * requires the customer's registered WhatsApp contact from {@code masterdata};
 * {@code PDF} reuses a shared document-rendering capability rather than a
 * dedicated outbound port (see {@code SendQuoteUseCase}'s spec notes).
 */
public enum QuoteDeliveryChannel {
	PDF,
	WHATSAPP
}
