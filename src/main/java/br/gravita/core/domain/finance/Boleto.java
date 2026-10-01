package br.gravita.core.domain.finance;

import br.gravita.core.domain.shared.BusinessRuleException;
import java.util.Objects;
import lombok.Getter;

@Getter
public final class Boleto {

	private static final int BARCODE_LINE_LENGTH = 47;

	private final BoletoId id;
	private final ReceivableId receivableId;
	private final BankIntegration bankIntegration;
	private final String barcodeLine;
	private final BoletoStatus status;

	public Boleto(BoletoId id, ReceivableId receivableId, BankIntegration bankIntegration, String barcodeLine,
			BoletoStatus status) {
		this.id = Objects.requireNonNull(id, "id is required");
		this.receivableId = Objects.requireNonNull(receivableId, "receivableId is required");
		this.bankIntegration = Objects.requireNonNull(bankIntegration, "bankIntegration is required");
		this.barcodeLine = requireValidBarcodeLine(barcodeLine);
		this.status = Objects.requireNonNull(status, "status is required");
	}

	public static Boleto issue(BoletoId id, ReceivableId receivableId, BankIntegration bankIntegration,
			String barcodeLine) {
		return new Boleto(id, receivableId, bankIntegration, barcodeLine, BoletoStatus.ISSUED);
	}

	public static Boleto of(BoletoId id, ReceivableId receivableId, BankIntegration bankIntegration,
			String barcodeLine, BoletoStatus status) {
		return new Boleto(id, receivableId, bankIntegration, barcodeLine, status);
	}

	/**
	 * A bank-title "linha digitável": 47 digits (punctuation is ignored and
	 * stripped), whose first three fields each end in a mod-10 check digit.
	 */
	private static String requireValidBarcodeLine(String barcodeLine) {
		if (barcodeLine == null || barcodeLine.isBlank()) {
			throw new BusinessRuleException("barcodeLine is required");
		}
		String digits = barcodeLine.replaceAll("[\\s.]", "");
		if (!digits.matches("\\d{" + BARCODE_LINE_LENGTH + "}")) {
			throw new BusinessRuleException("barcodeLine must have " + BARCODE_LINE_LENGTH + " digits: " + barcodeLine);
		}
		if (!hasValidFieldCheckDigit(digits, 0, 9) || !hasValidFieldCheckDigit(digits, 10, 20)
				|| !hasValidFieldCheckDigit(digits, 21, 31)) {
			throw new BusinessRuleException("barcodeLine has an invalid check digit: " + barcodeLine);
		}
		return digits;
	}

	/** Checks the mod-10 digit at {@code digits[to]} against the field {@code digits[from, to)}. */
	private static boolean hasValidFieldCheckDigit(String digits, int from, int to) {
		int sum = 0;
		int weight = 2;
		for (int i = to - 1; i >= from; i--) {
			int product = (digits.charAt(i) - '0') * weight;
			sum += product / 10 + product % 10;
			weight = weight == 2 ? 1 : 2;
		}
		return (10 - sum % 10) % 10 == digits.charAt(to) - '0';
	}
}
