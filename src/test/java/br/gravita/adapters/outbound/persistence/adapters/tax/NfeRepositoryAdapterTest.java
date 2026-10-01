package br.gravita.adapters.outbound.persistence.adapters.tax;

import static org.assertj.core.api.Assertions.assertThat;

import br.gravita.adapters.outbound.persistence.mappers.tax.NfePersistenceMapperImpl;
import br.gravita.adapters.outbound.persistence.repositories.tax.NfeJpaRepository;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.shared.PersonRef;
import br.gravita.core.domain.shared.PersonType;
import br.gravita.core.domain.tax.Cfop;
import br.gravita.core.domain.tax.ItemTaxBreakdown;
import br.gravita.core.domain.tax.NaturezaOperacao;
import br.gravita.core.domain.tax.NfeDocument;
import br.gravita.core.domain.tax.NfeDocumentId;
import br.gravita.core.domain.tax.NfeItem;
import br.gravita.core.domain.tax.NfeRecipient;
import br.gravita.core.domain.tax.NfeTransportInfo;
import br.gravita.core.domain.tax.TaxCalculationTotals;
import br.gravita.core.domain.tax.TaxLineBreakdown;
import br.gravita.core.domain.tax.TaxType;
import br.gravita.core.domain.tax.TransportModality;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({NfeRepositoryAdapter.class, NfePersistenceMapperImpl.class})
class NfeRepositoryAdapterTest {

	@Autowired
	private NfeRepositoryAdapter repositoryAdapter;

	@Autowired
	private NfeJpaRepository jpaRepository;

	@Test
	@DisplayName("Saves and reloads a queued NF-e preserving its items, tax lines and transport")
	void savesAndReloadsAQueuedDocumentPreservingItemsTaxLinesAndTransport() {
		UUID productId = UUID.randomUUID();
		TaxLineBreakdown icmsLine = new TaxLineBreakdown(TaxType.ICMS, new BigDecimal("100.00"), new BigDecimal("18"),
				new BigDecimal("18.00"), new BigDecimal("18.00"), false, null);
		TaxLineBreakdown ipiLine = new TaxLineBreakdown(TaxType.IPI, new BigDecimal("100.00"), new BigDecimal("5"),
				new BigDecimal("5.00"), new BigDecimal("4.00"), true, "Isenção parcial aprovada");
		ItemTaxBreakdown breakdown = new ItemTaxBreakdown(0, productId.toString(), List.of(icmsLine, ipiLine));
		NfeItem item = new NfeItem(productId, "Produto Teste", BigDecimal.ONE, new BigDecimal("100.00"),
				BigDecimal.ZERO, breakdown);

		NfeRecipient recipient = NfeRecipient.of(PersonRef.of(UUID.randomUUID()), "11.222.333/0001-81",
				PersonType.COMPANY, "Cliente PJ Teste", "123456789", "RJ");

		NfeTransportInfo transport = new NfeTransportInfo(TransportModality.CIF, "Transportadora Teste", 1,
				new BigDecimal("10.500"), new BigDecimal("10.000"), "12345678901");

		TaxCalculationTotals totals = TaxCalculationTotals.from(List.of(breakdown));

		NfeDocument draft = NfeDocument.draft(NfeDocumentId.of(UUID.randomUUID()), CompanyId.of(UUID.randomUUID()),
				null, NaturezaOperacao.VENDA, new Cfop("5102"), recipient, List.of(item), new BigDecimal("15.00"),
				BigDecimal.ZERO, BigDecimal.ZERO, transport, null, "Informação adicional de teste", totals,
				Instant.now());
		NfeDocument queued = draft.queue("001", 42L, "3".repeat(44));

		NfeDocument saved = repositoryAdapter.save(queued);
		Optional<NfeDocument> reloaded = repositoryAdapter.findById(saved.getId());

		assertThat(reloaded).isPresent();
		NfeDocument found = reloaded.get();
		assertThat(found.getAccessKey()).isEqualTo(queued.getAccessKey());
		assertThat(found.getDocumentNumber()).isEqualTo(42L);
		assertThat(found.getCfop()).isEqualTo(new Cfop("5102"));
		assertThat(found.getRecipient().name()).isEqualTo("Cliente PJ Teste");
		assertThat(found.getRecipient().state()).isEqualTo("RJ");
		assertThat(found.getTransport().modality()).isEqualTo(TransportModality.CIF);
		assertThat(found.getItems()).hasSize(1);
		assertThat(found.getItems().get(0).taxBreakdown().taxLines()).hasSize(2);
		assertThat(found.getTaxTotals().grandTotal()).isEqualByComparingTo("22.00");

		assertThat(jpaRepository.existsById(saved.getId().value())).isTrue();
	}
}
