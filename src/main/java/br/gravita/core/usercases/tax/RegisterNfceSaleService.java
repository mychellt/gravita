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

	public RegisterNfceSaleService(final PosSessionRepositoryPort posSessionRepositoryPort,
			final NfceRepositoryPort nfceRepositoryPort, final PriceTableRepositoryPort priceTableRepositoryPort) {
		this.posSessionRepositoryPort = posSessionRepositoryPort;
		this.nfceRepositoryPort = nfceRepositoryPort;
		this.priceTableRepositoryPort = priceTableRepositoryPort;
	}

	@Override
	public NfceSaleId execute(final RegisterNfceSaleCommand command) {
		final PosSessionId sessionId = PosSessionId.of(command.sessionId());
		final PosSession session = posSessionRepositoryPort.findById(sessionId)
				.orElseThrow(() -> new ResourceNotFoundException("PosSession not found: " + command.sessionId()));
		if (session.getStatus() != PosSessionStatus.OPEN) {
			throw new BusinessRuleException("PosSession " + command.sessionId() + " is not open");
		}

		final PriceTable priceTable = resolvePriceTable(command.priceTableId());

		final List<SaleItem> items = command.items().stream().map(itemCommand -> toSaleItem(itemCommand, priceTable)).toList();

		final BigDecimal subtotal = items.stream().map(SaleItem::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
		checkDiscount(priceTable, command.totalDiscount(), subtotal);

		final List<Payment> payments = command.payments().stream().map(this::toPayment).toList();

		final NfceSale sale = NfceSale.register(NfceSaleId.of(UUID.randomUUID()), sessionId, items, command.totalDiscount(),
				payments, command.customerCpf(), Instant.now());

		return nfceRepositoryPort.save(sale).getId();
	}

	private SaleItem toSaleItem(final SaleItemCommand itemCommand, final PriceTable priceTable) {
		final SaleItem item = new SaleItem(itemCommand.productId(), itemCommand.quantity(), itemCommand.unitPrice(),
				itemCommand.itemDiscount());
		checkDiscount(priceTable, itemCommand.itemDiscount(), item.subtotal());
		return item;
	}

	private Payment toPayment(final PaymentCommand paymentCommand) {
		return new Payment(paymentCommand.method(), paymentCommand.amount());
	}

	private void checkDiscount(final PriceTable priceTable, final BigDecimal discount, final BigDecimal base) {
		if (priceTable == null || discount == null || discount.compareTo(BigDecimal.ZERO) <= 0
				|| base.compareTo(BigDecimal.ZERO) <= 0) {
			return;
		}
		final BigDecimal discountPercent = discount.divide(base, 4, RoundingMode.HALF_UP).multiply(ONE_HUNDRED);
		priceTable.evaluateDiscount(discountPercent);
	}

	private PriceTable resolvePriceTable(final UUID priceTableId) {
		if (priceTableId == null) {
			return null;
		}
		return priceTableRepositoryPort.findById(PriceTableId.of(priceTableId))
				.orElseThrow(() -> new ResourceNotFoundException("PriceTable not found: " + priceTableId));
	}
}
