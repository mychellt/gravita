package br.gravita.core.domain.finance;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * The CNAB payment remittance (remessa) generated for a batch of approved
 * payables. {@code payableIds} references every payable it covers: the bank
 * echoes each one back as the title identifier of its return line, which is
 * how the return is matched to the payables (UC-M8-22).
 */
public record CnabRemittance(String reference, BankIntegration bankIntegration, List<PayableId> payableIds,
		BigDecimal totalAmount, String fileContent) {

	public CnabRemittance {
		if (reference == null || reference.isBlank()) {
			throw new IllegalArgumentException("reference is required");
		}
		Objects.requireNonNull(bankIntegration, "bankIntegration is required");
		Objects.requireNonNull(payableIds, "payableIds is required");
		if (payableIds.isEmpty()) {
			throw new IllegalArgumentException("payableIds must not be empty");
		}
		payableIds = List.copyOf(payableIds);
		Objects.requireNonNull(totalAmount, "totalAmount is required");
		if (fileContent == null || fileContent.isBlank()) {
			throw new IllegalArgumentException("fileContent is required");
		}
	}
}
