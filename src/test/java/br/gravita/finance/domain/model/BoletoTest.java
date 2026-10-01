package br.gravita.finance.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.gravita.core.domain.finance.BankIntegration;
import br.gravita.core.domain.finance.Boleto;
import br.gravita.core.domain.finance.BoletoId;
import br.gravita.core.domain.finance.BoletoStatus;
import br.gravita.core.domain.finance.ReceivableId;
import br.gravita.core.domain.shared.BusinessRuleException;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BoletoTest {

	static final String VALID_LINE = "00190500954014481606906809350314437370000000100";

	private static Boleto issue(String barcodeLine) {
		return Boleto.issue(BoletoId.of(UUID.randomUUID()), ReceivableId.of(UUID.randomUUID()),
				BankIntegration.BANCO_DO_BRASIL, barcodeLine);
	}

	@Test
	@DisplayName("Starts an issued boleto in the issued status carrying its barcode line")
	void anIssuedBoletoCarriesItsBarcodeLineAndStartsIssued() {
		Boleto boleto = issue(VALID_LINE);

		assertThat(boleto.getBarcodeLine()).isEqualTo(VALID_LINE);
		assertThat(boleto.getStatus()).isEqualTo(BoletoStatus.ISSUED);
		assertThat(boleto.getBankIntegration()).isEqualTo(BankIntegration.BANCO_DO_BRASIL);
	}

	@Test
	@DisplayName("Strips punctuation from the barcode line")
	void punctuationInTheBarcodeLineIsStripped() {
		Boleto boleto = issue("00190.50095 40144.816069 06809.350314 4 37370000000100");

		assertThat(boleto.getBarcodeLine()).isEqualTo(VALID_LINE);
	}

	@Test
	@DisplayName("Rejects a barcode line of the wrong length")
	void rejectsABarcodeLineOfTheWrongLength() {
		assertThatThrownBy(() -> issue("0019050095")).isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("47 digits");
	}

	@Test
	@DisplayName("Rejects a barcode line that is not numeric")
	void rejectsANonNumericBarcodeLine() {
		assertThatThrownBy(() -> issue("A".repeat(47))).isInstanceOf(BusinessRuleException.class);
	}

	@Test
	@DisplayName("Rejects a barcode line with an invalid field check digit")
	void rejectsABarcodeLineWithABadFieldCheckDigit() {
		// first field's check digit (10th digit) changed from 5 to 6
		assertThatThrownBy(() -> issue("00190500964014481606906809350314437370000000100"))
				.isInstanceOf(BusinessRuleException.class).hasMessageContaining("check digit");
	}

	@Test
	@DisplayName("Rejects a missing barcode line")
	void rejectsAMissingBarcodeLine() {
		assertThatThrownBy(() -> issue(null)).isInstanceOf(BusinessRuleException.class);
	}
}
