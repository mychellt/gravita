package br.gravita.tax.domain.model;

import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.domain.tax.InboundNfe;
import br.gravita.core.domain.tax.InboundNfeId;
import br.gravita.core.domain.tax.InboundNfeItem;
import br.gravita.core.domain.tax.InboundNfeStatus;
import br.gravita.core.domain.tax.InboundNfeTotals;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InboundNfeTest {

	private static final CompanyId COMPANY_ID = CompanyId.of(UUID.randomUUID());
	private static final String ACCESS_KEY = "35240111222333000181550010000012345123456789";

	@Test
	void importingFromXmlStartsPendingConference() {
		InboundNfe inboundNfe = InboundNfe.importedFromXml(InboundNfeId.of(UUID.randomUUID()), COMPANY_ID,
				ACCESS_KEY, "1", "12345", Document.cnpj("11222333000181"), "Fornecedor Exemplo LTDA", Instant.now(),
				List.of(item()), totals(), "xml-ref-1");

		assertThat(inboundNfe.getStatus()).isEqualTo(InboundNfeStatus.PENDING_CONFERENCE);
		assertThat(inboundNfe.getItems()).hasSize(1);
		assertThat(inboundNfe.getXmlStorageRef()).isEqualTo("xml-ref-1");
	}

	@Test
	void enteringManuallyProducesTheSamePendingConferenceShapeAsXmlImport() {
		InboundNfe inboundNfe = InboundNfe.enteredManually(InboundNfeId.of(UUID.randomUUID()), COMPANY_ID, ACCESS_KEY,
				"1", "12345", Document.cnpj("11222333000181"), "Fornecedor Exemplo LTDA", Instant.now(),
				List.of(item()), totals());

		assertThat(inboundNfe.getStatus()).isEqualTo(InboundNfeStatus.PENDING_CONFERENCE);
		assertThat(inboundNfe.getItems()).hasSize(1);
		assertThat(inboundNfe.getXmlStorageRef()).isNotBlank();
	}

	@Test
	void rejectsAnAccessKeyThatIsNot44Digits() {
		assertThatThrownBy(() -> InboundNfe.importedFromXml(InboundNfeId.of(UUID.randomUUID()), COMPANY_ID, "12345",
				"1", "12345", Document.cnpj("11222333000181"), "Fornecedor", Instant.now(), List.of(item()),
				totals(), "xml-ref-1"))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("44 digits");
	}

	@Test
	void rejectsAnEmptyItemList() {
		assertThatThrownBy(() -> InboundNfe.importedFromXml(InboundNfeId.of(UUID.randomUUID()), COMPANY_ID,
				ACCESS_KEY, "1", "12345", Document.cnpj("11222333000181"), "Fornecedor", Instant.now(), List.of(),
				totals(), "xml-ref-1"))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("at least one item");
	}

	@Test
	void rejectsAMissingXmlStorageRef() {
		assertThatThrownBy(() -> InboundNfe.importedFromXml(InboundNfeId.of(UUID.randomUUID()), COMPANY_ID,
				ACCESS_KEY, "1", "12345", Document.cnpj("11222333000181"), "Fornecedor", Instant.now(),
				List.of(item()), totals(), " "))
				.isInstanceOf(BusinessRuleException.class);
	}

	private InboundNfeItem item() {
		return new InboundNfeItem("SKU-001", "Parafuso Sextavado M8", "73181500", "5102", "UN",
				new BigDecimal("100.0000"), new BigDecimal("1.5000"), new BigDecimal("150.00"),
				new BigDecimal("27.00"), BigDecimal.ZERO, new BigDecimal("2.48"), new BigDecimal("11.40"));
	}

	private InboundNfeTotals totals() {
		return new InboundNfeTotals(new BigDecimal("150.00"), new BigDecimal("15.00"), BigDecimal.ZERO,
				BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("27.00"), BigDecimal.ZERO, new BigDecimal("2.48"),
				new BigDecimal("11.40"), new BigDecimal("165.00"));
	}
}
