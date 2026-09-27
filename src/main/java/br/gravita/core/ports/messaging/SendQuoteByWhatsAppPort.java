package br.gravita.core.ports.messaging;

/**
 * UC-M7-02: delivers a quote to the customer's registered WhatsApp contact.
 */
public interface SendQuoteByWhatsAppPort {

	void send(SendQuoteByWhatsAppRequest request);
}
