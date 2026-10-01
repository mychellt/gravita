package br.gravita.adapters.outbound.persistence.adapters.tax;

import br.gravita.adapters.outbound.persistence.repositories.tax.XmlObjectJpaRepository;
import br.gravita.core.domain.masterdata.CompanyId;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(XmlObjectStorageAdapter.class)
class XmlObjectStorageAdapterTest {

	@Autowired
	private XmlObjectStorageAdapter storageAdapter;

	@Autowired
	private XmlObjectJpaRepository jpaRepository;

	@Test
	@DisplayName("Returns a reference that resolves to the original content after storing an XML")
	void storingXmlReturnsAReferenceThatResolvesToTheOriginalContent() {
		CompanyId companyId = CompanyId.of(UUID.randomUUID());
		byte[] xml = "<NFe>example</NFe>".getBytes(StandardCharsets.UTF_8);

		String reference = storageAdapter.store(companyId, xml);

		assertThat(reference).isNotBlank();
		var stored = jpaRepository.findById(UUID.fromString(reference)).orElseThrow();
		assertThat(stored.getCompanyId()).isEqualTo(companyId.value());
		assertThat(stored.getContent()).isEqualTo(xml);
	}
}
