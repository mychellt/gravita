package br.gravita.core.usercases.tax;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.masterdata.PriceTable;
import br.gravita.core.domain.masterdata.PriceTableId;
import br.gravita.core.domain.tax.NfceSale;
import br.gravita.core.domain.tax.NfceSaleId;
import br.gravita.core.domain.tax.Payment;
import br.gravita.core.domain.tax.PosSession;
import br.gravita.core.domain.tax.PosSessionId;
import br.gravita.core.domain.tax.PosSessionStatus;
import br.gravita.core.domain.tax.SaleItem;
import br.gravita.core.ports.inbound.tax.RegisterNfceSaleCommand;
import br.gravita.core.ports.inbound.tax.RegisterNfceSaleCommand.PaymentCommand;
import br.gravita.core.ports.inbound.tax.RegisterNfceSaleCommand.SaleItemCommand;
import br.gravita.core.ports.inbound.tax.RegisterNfceSaleUseCase;
import br.gravita.core.ports.outbound.persistence.PriceTableRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.NfceRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.PosSessionRepositoryPort;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@UseCase
public class RegisterNfceSaleService implements RegisterNfceSaleUseCase {

	private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

	private final PosSessionRepositoryPort posSessionRepositoryPort;
	private final NfceRepositoryPort nfceRepositoryPort;
	private final PriceTableRepositoryPort priceTableRepositoryPort;

	public RegisterNfceSaleService(PosSessionRepositoryPort posSessionRepositoryPort,
			NfceRepositoryPort nfceRepositoryPort, PriceTableRepositoryPort priceTableRepositoryPort) {
		this.posSessionRepositoryPort = posSessionRepositoryPort;
		this.nfceRepositoryPort = nfceRepositoryPort;
		this.priceTableRepositoryPort = priceTableRepositoryPort;
	}

	@Override
	public NfceSaleId execute(RegisterNfceSaleCommand command) {
		PosSessionId sessionId = PosSessionId.of(command.sessionId());
		PosSession session = posSessionRepositoryPort.findById(sessionId)
				.orElseThrow(() -> new ResourceNotFoundException("PosSession not found: " + command.sessionId()));
		if (session.getStatus() != PosSessionStatus.OPEN) {
			throw new BusinessRuleException("PosSession " + command.sessionId() + " is not open");
		}

		PriceTable priceTable = resolvePriceTable(command.priceTableId());

		List<SaleItem> items = command.items().stream().map(itemCommand -> toSaleItem(itemCommand, priceTable)).toList();

		BigDecimal subtotal = items.stream().map(SaleItem::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
		checkDiscount(priceTable, command.totalDiscount(), subtotal);

		List<Payment> payments = command.payments().stream().map(this::toPayment).toList();

		NfceSale sale = NfceSale.register(NfceSaleId.of(UUID.randomUUID()), sessionId, items, command.totalDiscount(),
				payments, command.customerCpf(), Instant.now());

		return nfceRepositoryPort.save(sale).getId();
	}

	private SaleItem toSaleItem(SaleItemCommand itemCommand, PriceTable priceTable) {
		SaleItem item = new SaleItem(itemCommand.productId(), itemCommand.quantity(), itemCommand.unitPrice(),
				itemCommand.itemDiscount());
		checkDiscount(priceTable, itemCommand.itemDiscount(), item.subtotal());
		return item;
	}

	private Payment toPayment(PaymentCommand paymentCommand) {
		return new Payment(paymentCommand.method(), paymentCommand.amount());
	}

	/**
	 * AC2: caps {@code discount} (a money amount) against the linked price
	 * table's {@code maxDiscountPercent} by converting it to a percentage of
	 * {@code base} first, since {@link PriceTable#evaluateDiscount} works in
	 * percentage terms. A {@code BLOCK} table throws; an {@code ALERT} table
	 * lets the sale proceed (the use case has no channel back to the cashier
	 * for a non-blocking toast, so the ALERT outcome is intentionally
	 * discarded here).
	 */
	private void checkDiscount(PriceTable priceTable, BigDecimal discount, BigDecimal base) {
		if (priceTable == null || discount == null || discount.compareTo(BigDecimal.ZERO) <= 0
				|| base.compareTo(BigDecimal.ZERO) <= 0) {
			return;
		}
		BigDecimal discountPercent = discount.divide(base, 4, RoundingMode.HALF_UP).multiply(ONE_HUNDRED);
		priceTable.evaluateDiscount(discountPercent);
	}

	private PriceTable resolvePriceTable(UUID priceTableId) {
		if (priceTableId == null) {
			return null;
		}
		return priceTableRepositoryPort.findById(PriceTableId.of(priceTableId))
				.orElseThrow(() -> new ResourceNotFoundException("PriceTable not found: " + priceTableId));
	}
}
