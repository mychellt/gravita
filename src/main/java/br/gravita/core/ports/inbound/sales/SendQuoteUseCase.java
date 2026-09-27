package br.gravita.core.ports.inbound.sales;

/**
 * UC-M7-02: delivers an existing quote to the customer, either as a PDF
 * document or via WhatsApp. On successful delivery the quote transitions to
 * {@code SENT}; delivery of an already-expired quote is rejected.
 */
public interface SendQuoteUseCase {

	void execute(SendQuoteCommand command);
}
