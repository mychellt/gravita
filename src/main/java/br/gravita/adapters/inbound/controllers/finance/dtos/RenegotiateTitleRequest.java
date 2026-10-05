package br.gravita.adapters.inbound.controllers.finance.dtos;

import br.gravita.core.ports.inbound.finance.RenegotiateTitleCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @param additionalReceivableIds further overdue titles of the same customer to
 *                                renegotiate together with the one in the path
 * @param installments            the agreed plan, in installment order
 */
public record RenegotiateTitleRequest(
		List<@NotNull UUID> additionalReceivableIds,
		@NotEmpty List<@NotNull @Valid InstallmentRequest> installments) {

	public record InstallmentRequest(@NotNull LocalDate dueDate, @NotNull @Positive BigDecimal amount) {
	}

	public RenegotiateTitleCommand toCommand(final UUID receivableId) {
		final List<UUID> originals = new ArrayList<>();
		originals.add(receivableId);
		if (additionalReceivableIds != null) {
			originals.addAll(additionalReceivableIds);
		}
		return new RenegotiateTitleCommand(originals, installments.stream()
				.map(i -> new RenegotiateTitleCommand.Installment(i.dueDate(), i.amount())).toList());
	}
}
