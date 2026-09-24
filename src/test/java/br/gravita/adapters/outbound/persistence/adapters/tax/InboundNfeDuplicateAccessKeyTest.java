package br.gravita.adapters.outbound.persistence.adapters.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.InboundNfeJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.tax.InboundNfePersistenceMapper;
import br.gravita.adapters.outbound.persistence.mappers.tax.InboundNfePersistenceMapperImpl;
import br.gravita.adapters.outbound.persistence.repositories.tax.InboundNfeJpaRepository;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.domain.tax.InboundNfe;
import br.gravita.core.domain.tax.InboundNfeId;
import br.gravita.core.domain.tax.InboundNfeItem;
import br.gravita.core.domain.tax.InboundNfeTotals;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * GRA-68 QA finding: {@code access_key} is unique at the DB level (V26
 * migration / {@code InboundNfeJpaEntity#accessKey}), but nothing between
 * {@link InboundNfeRepositoryAdapter} and {@code InboundNfeController}
 * translates the resulting {@link DataIntegrityViolationException} into a
 * {@code BusinessRuleException}. Confirmed at the HTTP layer: re-uploading
 * an XML with an access key already on file returns a raw 500 with a full
 * stack trace in the JSON body instead of a clean 4xx. This test locks in
 * today's (undesired) behaviour so it fails loudly - and needs updating -
 * once the duplicate is translated into a proper domain exception.
 */
@DataJpaTest
@Import({InboundNfeRepositoryAdapter.class, InboundNfePersistenceMapperImpl.class})
class InboundNfeDuplicateAccessKeyTest {

	@Autowired
	private InboundNfeJpaRepository jpaRepository;

	@Autowired
	private InboundNfePersistenceMapper mapper;

	@Test
	void savingASecondInboundNfeWithAnAlreadyUsedAccessKeyThrowsAnUntranslatedPersistenceException() {
		CompanyId companyId = CompanyId.of(UUID.randomUUID());
		String accessKey = "35240111222333000181550010000012345123456789";

		InboundNfeJpaEntity first = mapper.toEntity(inboundNfe(companyId, accessKey, "xml-object-ref-1"));
		jpaRepository.saveAndFlush(first);

		InboundNfeJpaEntity duplicate = mapper.toEntity(inboundNfe(companyId, accessKey, "xml-object-ref-2"));
		assertThatThrownBy(() -> jpaRepository.saveAndFlush(duplicate))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	private InboundNfe inboundNfe(CompanyId companyId, String accessKey, String xmlStorageRef) {
		return InboundNfe.importedFromXml(InboundNfeId.of(UUID.randomUUID()), companyId, accessKey, "1", "12345",
				Document.cnpj("11222333000181"), "Fornecedor Exemplo LTDA",
				Instant.now().truncatedTo(ChronoUnit.MILLIS),
				List.of(new InboundNfeItem("SKU-001", "Parafuso Sextavado M8", "73181500", "5102", "UN",
						new BigDecimal("100.0000"), new BigDecimal("1.5000"), new BigDecimal("150.00"),
						new BigDecimal("27.00"), BigDecimal.ZERO, new BigDecimal("2.48"), new BigDecimal("11.40"))),
				new InboundNfeTotals(new BigDecimal("150.00"), new BigDecimal("15.00"), BigDecimal.ZERO,
						BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("27.00"), BigDecimal.ZERO,
						new BigDecimal("2.48"), new BigDecimal("11.40"), new BigDecimal("165.00")),
				xmlStorageRef);
	}
}
