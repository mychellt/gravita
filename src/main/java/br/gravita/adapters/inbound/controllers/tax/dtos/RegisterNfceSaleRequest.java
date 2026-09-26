package br.gravita.adapters.inbound.controllers.tax.dtos;

import br.gravita.core.domain.PaymentMethodType;
import br.gravita.core.ports.inbound.tax.RegisterNfceSaleCommand;
import br.gravita.core.ports.inbound.tax.RegisterNfceSaleCommand.PaymentCommand;
import br.gravita.core.ports.inbound.tax.RegisterNfceSaleCommand.SaleItemCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record RegisterNfceSaleRequest(@NotNull UUID sessionId, @NotEmpty List<@Valid ItemRequest> items,
		BigDecimal totalDiscount, @NotEmpty List<@Valid PaymentRequest> payments, String customerCpf,
		UUID priceTableId) {

	public RegisterNfceSaleCommand toCommand() {
		return new RegisterNfceSaleCommand(sessionId, items.stream().map(ItemRequest::toCommand).toList(),
				totalDiscount, payments.stream().map(PaymentRequest::toCommand).toList(), customerCpf, priceTableId);
	}

	public record ItemRequest(@NotNull UUID productId, @NotNull BigDecimal quantity, @NotNull BigDecimal unitPrice,
			BigDecimal itemDiscount) {

		SaleItemCommand toCommand() {
			return new SaleItemCommand(productId, quantity, unitPrice, itemDiscount);
		}
	}

	public record PaymentRequest(@NotNull PaymentMethodType method, @NotNull BigDecimal amount) {

		PaymentCommand toCommand() {
			return new PaymentCommand(method, amount);
		}
	}
}
