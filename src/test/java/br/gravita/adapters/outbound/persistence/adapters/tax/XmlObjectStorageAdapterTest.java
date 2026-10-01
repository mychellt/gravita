package br.gravita.adapters.outbound.persistence.adapters.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.XmlObjectJpaEntity;
import br.gravita.adapters.outbound.persistence.repositories.tax.XmlObjectJpaRepository;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.masterdata.CompanyId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class XmlObjectStorageAdapterTest {

	@Mock
	private XmlObjectJpaRepository repository;

	@InjectMocks
	private XmlObjectStorageAdapter adapter;

	@Test
	@DisplayName("Returns a reference that resolves to the stored object after storing an XML")
	void storingXmlReturnsAReferenceThatResolvesToTheStoredObject() {
		final CompanyId companyId = CompanyId.of(UUID.randomUUID());
		final byte[] xml = "<NFe>example</NFe>".getBytes(StandardCharsets.UTF_8);
		when(repository.save(any(XmlObjectJpaEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		final String reference = adapter.store(companyId, xml);

		final ArgumentCaptor<XmlObjectJpaEntity> captor = ArgumentCaptor.forClass(XmlObjectJpaEntity.class);
		verify(repository).save(captor.capture());
		assertThat(reference).isEqualTo(captor.getValue().getId().toString());
		assertThat(captor.getValue().getCompanyId()).isEqualTo(companyId.value());
		assertThat(captor.getValue().getContent()).isEqualTo(xml);
		assertThat(captor.getValue().isNew()).isTrue();
	}

	@Test
	@DisplayName("Retrieves the stored content by its reference")
	void retrievesTheStoredContentByItsReference() {
		final UUID id = UUID.randomUUID();
		final byte[] xml = "<NFe>example</NFe>".getBytes(StandardCharsets.UTF_8);
		when(repository.findById(id)).thenReturn(Optional.of(XmlObjectJpaEntity.builder().id(id).content(xml).build()));

		assertThat(adapter.retrieve(id.toString())).isEqualTo(xml);
	}

	@Test
	@DisplayName("Throws ResourceNotFoundException when the reference does not match a stored object")
	void throwsWhenTheReferenceDoesNotMatchAStoredObject() {
		final UUID id = UUID.randomUUID();
		when(repository.findById(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> adapter.retrieve(id.toString())).isInstanceOf(ResourceNotFoundException.class);
	}
}
