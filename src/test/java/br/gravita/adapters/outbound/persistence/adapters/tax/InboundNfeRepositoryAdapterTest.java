package br.gravita.adapters.outbound.persistence.adapters.tax;

import br.gravita.adapters.outbound.persistence.mappers.tax.InboundNfePersistenceMapperImpl;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.domain.tax.InboundNfe;
import br.gravita.core.domain.tax.InboundNfeConferenceItem;
import br.gravita.core.domain.tax.InboundNfeId;
import br.gravita.core.domain.tax.InboundNfeItem;
import br.gravita.core.domain.tax.InboundNfeTotals;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({InboundNfeRepositoryAdapter.class, InboundNfePersistenceMapperImpl.class})
class InboundNfeRepositoryAdapterTest {

	@Autowired
	private InboundNfeRepositoryAdapter repositoryAdapter;

	@Test
	@DisplayName("Persists an inbound NF-e with its supplier, items and totals")
	void savingAnInboundNfePersistsSupplierItemsAndTotals() {
		CompanyId companyId = CompanyId.of(UUID.randomUUID());
		InboundNfe inboundNfe = InboundNfe.importedFromXml(InboundNfeId.of(UUID.randomUUID()), companyId,
				"35240111222333000181550010000012345123456789", "1", "12345", Document.cnpj("11222333000181"),
				"Fornecedor Exemplo LTDA", Instant.now().truncatedTo(ChronoUnit.MILLIS),
				List.of(new InboundNfeItem("SKU-001", "Parafuso Sextavado M8", "73181500", "5102", "UN",
						new BigDecimal("100.0000"), new BigDecimal("1.5000"), new BigDecimal("150.00"),
						new BigDecimal("27.00"), BigDecimal.ZERO, new BigDecimal("2.48"), new BigDecimal("11.40"))),
				new InboundNfeTotals(new BigDecimal("150.00"), new BigDecimal("15.00"), BigDecimal.ZERO,
						BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("27.00"), BigDecimal.ZERO,
						new BigDecimal("2.48"), new BigDecimal("11.40"), new BigDecimal("165.00")),
				"xml-object-ref-1");

		InboundNfe saved = repositoryAdapter.save(inboundNfe);

		assertThat(saved.getId()).isEqualTo(inboundNfe.getId());
		assertThat(saved.getSupplierDocument()).isEqualTo(Document.cnpj("11222333000181"));
		assertThat(saved.getSupplierName()).isEqualTo("Fornecedor Exemplo LTDA");
		assertThat(saved.getXmlStorageRef()).isEqualTo("xml-object-ref-1");
		assertThat(saved.getItems()).hasSize(1);
		assertThat(saved.getItems().get(0).supplierProductCode()).isEqualTo("SKU-001");
		assertThat(saved.getTotals().totalValue()).isEqualByComparingTo("165.00");
	}

	@Test
	@DisplayName("Finds only the company's confirmed NF-e issued within the window, oldest first")
	void findsOnlyTheConfirmedNfeOfTheCompanyIssuedWithinTheWindowOldestFirst() {
		CompanyId company = CompanyId.of(UUID.randomUUID());
		Instant from = Instant.parse("2028-02-01T00:00:00Z");
		Instant to = Instant.parse("2028-03-01T00:00:00Z");
		InboundNfe later = saveInbound(company, "35240111222333000181550010000000200123456789",
				"2028-02-20T12:00:00Z", true);
		InboundNfe earlier = saveInbound(company, "35240111222333000181550010000000100123456789",
				"2028-02-05T12:00:00Z", true);
		saveInbound(company, "35240111222333000181550010000000300123456789", "2028-02-06T12:00:00Z", false);
		saveInbound(company, "35240111222333000181550010000000400123456789", "2028-03-01T00:00:00Z", true);
		saveInbound(CompanyId.of(UUID.randomUUID()), "35240111222333000181550010000000500123456789",
				"2028-02-06T12:00:00Z", true);

		assertThat(repositoryAdapter.findConfirmedByCompanyBetween(company, from, to)).extracting(InboundNfe::getId)
				.containsExactly(earlier.getId(), later.getId());
	}

	private InboundNfe saveInbound(CompanyId company, String accessKey, String issuedAt, boolean confirmed) {
		BigDecimal total = new BigDecimal("10.00");
		InboundNfeItem item = new InboundNfeItem("SKU", "Item", "73181500", "1102", "UN", BigDecimal.ONE, total, total,
				BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
		InboundNfe nfe = InboundNfe.importedFromXml(InboundNfeId.of(UUID.randomUUID()), company, accessKey, "1", "1",
				Document.cnpj("11222333000181"), "Fornecedor", Instant.parse(issuedAt), List.of(item),
				new InboundNfeTotals(total, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
						BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, total),
				"xml-ref");
		if (confirmed) {
			nfe = nfe.confirm(List.of(new InboundNfeConferenceItem(UUID.randomUUID(), BigDecimal.ONE,
					BigDecimal.ONE)));
		}
		return repositoryAdapter.save(nfe);
	}
}
