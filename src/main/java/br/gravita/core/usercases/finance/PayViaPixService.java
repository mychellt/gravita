package br.gravita.core.usercases.finance;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.finance.Payable;
import br.gravita.core.domain.finance.PayableId;
import br.gravita.core.domain.finance.PayableStatus;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.finance.PayViaPixCommand;
import br.gravita.core.ports.inbound.finance.PayViaPixUseCase;
import br.gravita.core.ports.outbound.finance.BankIntegrationPort;
import br.gravita.core.ports.outbound.finance.BankIntegrationPort.PixPaymentReceipt;
import br.gravita.core.ports.outbound.finance.BankIntegrationPort.PixPaymentRequest;
import br.gravita.core.ports.outbound.finance.DocumentAttachmentStoragePort;
import br.gravita.core.ports.outbound.finance.DocumentAttachmentStoragePort.Document;
import br.gravita.core.ports.outbound.finance.DocumentAttachmentStoragePort.DocumentStorageUnavailableException;
import br.gravita.core.ports.outbound.persistence.finance.PayableRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@UseCase
@Slf4j
public class PayViaPixService implements PayViaPixUseCase {

	private final PayableRepositoryPort payableRepositoryPort;
	private final BankIntegrationPort bankIntegrationPort;
	private final DocumentAttachmentStoragePort documentAttachmentStoragePort;

	// Not transactional on purpose: the bank call must not hold a database transaction open, and the
	// payable is only written once, after the money has moved.
	@Override
	public Payable execute(final PayViaPixCommand command) {
		final Payable payable = payableRepositoryPort.findById(PayableId.of(command.payableId()))
				.orElseThrow(() -> new ResourceNotFoundException("Payable not found: " + command.payableId()));
		// Checked before the bank is contacted so a rejected payment never moves money.
		if (payable.getStatus() != PayableStatus.APPROVED) {
			throw new BusinessRuleException("Only APPROVED payables can be paid via PIX: payable "
					+ payable.getId().value() + " is " + payable.getStatus());
		}

		// A failed transfer throws here, before anything is written: the payable stays APPROVED.
		final PixPaymentReceipt receipt = bankIntegrationPort.payViaPix(
				new PixPaymentRequest(payable.getId().value(), payable.getScope(), command.pixKey(),
						payable.getAmount()));

		return payableRepositoryPort.save(payable.pay(storeReceipt(payable, receipt)));
	}

	/**
	 * The money has already moved, so a receipt that can't be stored must not
	 * keep the payable {@code APPROVED} (it would be paid again): it is recorded
	 * as paid without the attachment and can still be attached later (UC-M8-15).
	 */
	private String storeReceipt(final Payable payable, final PixPaymentReceipt receipt) {
		try {
			return documentAttachmentStoragePort.store(new Document(
					"pix-receipt-" + receipt.endToEndId() + extensionOf(receipt), receipt.contentType(),
					receipt.content()));
		} catch (final DocumentStorageUnavailableException exception) {
			log.error("PIX payment {} of payable {} went through but its receipt could not be stored",
					receipt.endToEndId(), payable.getId().value(), exception);
			return null;
		}
	}

	private static String extensionOf(final PixPaymentReceipt receipt) {
		return "application/pdf".equals(receipt.contentType()) ? ".pdf" : "";
	}
}
