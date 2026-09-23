package br.gravita.core.ports.inbound.purchasing;

/**
 * UC-M6-03: records one supplier's reply to a {@code Quotation} - their price
 * per item and delivery deadline. Re-submission by the same supplier
 * replaces their prior response rather than adding a second one.
 */
public interface RegisterQuotationResponseUseCase {
	void execute(RegisterQuotationResponseCommand command);
}
