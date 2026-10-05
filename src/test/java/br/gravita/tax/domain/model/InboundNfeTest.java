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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InboundNfeTest {

	private static final CompanyId COMPANY_ID = CompanyId.of(UUID.randomUUID());
	private static final String ACCESS_KEY = "35240111222333000181550010000012345123456789";

	@Test
	@DisplayName("Importing from XML starts in pending conference")
	void importingFromXmlStartsPendingConference() {
		final InboundNfe inboundNfe = InboundNfe.importedFromXml()
				.id(InboundNfeId.of(UUID.randomUUID()))
				.companyId(COMPANY_ID)
				.accessKey(ACCESS_KEY)
				.series("1")
				.number("12345")
				.supplierDocument(Document.cnpj("11222333000181"))
				.supplierName("Fornecedor Exemplo LTDA")
				.issuedAt(Instant.now())
				.items(List.of(item()))
				.totals(totals())
				.xmlStorageRef("xml-ref-1")
				.build();

		assertThat(inboundNfe.getStatus()).isEqualTo(InboundNfeStatus.PENDING_CONFERENCE);
		assertThat(inboundNfe.getItems()).hasSize(1);
		assertThat(inboundNfe.getXmlStorageRef()).isEqualTo("xml-ref-1");
	}

	@Test
	@DisplayName("Manual entry produces the same pending-conference shape as an XML import")
	void enteringManuallyProducesTheSamePendingConferenceShapeAsXmlImport() {
		final InboundNfe inboundNfe = InboundNfe.enteredManually(InboundNfeId.of(UUID.randomUUID()), COMPANY_ID, ACCESS_KEY,
				"1", "12345", Document.cnpj("11222333000181"), "Fornecedor Exemplo LTDA", Instant.now(),
				List.of(item()), totals());

		assertThat(inboundNfe.getStatus()).isEqualTo(InboundNfeStatus.PENDING_CONFERENCE);
		assertThat(inboundNfe.getItems()).hasSize(1);
		assertThat(inboundNfe.getXmlStorageRef()).isNotBlank();
	}

	@Test
	@DisplayName("Rejects an access key that is not 44 digits")
	void rejectsAnAccessKeyThatIsNot44Digits() {
		assertThatThrownBy(() -> InboundNfe.importedFromXml()
				.id(InboundNfeId.of(UUID.randomUUID()))
				.companyId(COMPANY_ID)
				.accessKey("12345")
				.series("1")
				.number("12345")
				.supplierDocument(Document.cnpj("11222333000181"))
				.supplierName("Fornecedor")
				.issuedAt(Instant.now())
				.items(List.of(item()))
				.totals(totals())
				.xmlStorageRef("xml-ref-1")
				.build())
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("44 digits");
	}

	@Test
	@DisplayName("Rejects an empty item list")
	void rejectsAnEmptyItemList() {
		assertThatThrownBy(() -> InboundNfe.importedFromXml()
				.id(InboundNfeId.of(UUID.randomUUID()))
				.companyId(COMPANY_ID)
				.accessKey(ACCESS_KEY)
				.series("1")
				.number("12345")
				.supplierDocument(Document.cnpj("11222333000181"))
				.supplierName("Fornecedor")
				.issuedAt(Instant.now())
				.items(List.of())
				.totals(totals())
				.xmlStorageRef("xml-ref-1")
				.build())
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("at least one item");
	}

	@Test
	@DisplayName("Rejects a missing XML storage reference")
	void rejectsAMissingXmlStorageRef() {
		assertThatThrownBy(() -> InboundNfe.importedFromXml()
				.id(InboundNfeId.of(UUID.randomUUID()))
				.companyId(COMPANY_ID)
				.accessKey(ACCESS_KEY)
				.series("1")
				.number("12345")
				.supplierDocument(Document.cnpj("11222333000181"))
				.supplierName("Fornecedor")
				.issuedAt(Instant.now())
				.items(List.of(item()))
				.totals(totals())
				.xmlStorageRef(" ")
				.build())
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
