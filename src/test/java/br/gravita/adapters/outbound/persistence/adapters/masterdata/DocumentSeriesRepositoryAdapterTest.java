package br.gravita.adapters.outbound.persistence.adapters.masterdata;

import br.gravita.adapters.outbound.persistence.entities.masterdata.DocumentSeriesJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.masterdata.DocumentSeriesPersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.masterdata.DocumentSeriesJpaRepository;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.DocumentSeries;
import br.gravita.core.domain.masterdata.FiscalDocumentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentSeriesRepositoryAdapterTest {

	@Mock
	private DocumentSeriesJpaRepository repository;

	@Mock
	private DocumentSeriesPersistenceMapper mapper;

	@InjectMocks
	private DocumentSeriesRepositoryAdapter adapter;

	@Test
	@DisplayName("Saves a new document series marking its entity as new")
	void shouldSaveNewDocumentSeries() {
		final CompanyId companyId = CompanyId.of(UUID.randomUUID());
		final DocumentSeries series = DocumentSeries.placeholder(companyId, FiscalDocumentType.NFE);
		final DocumentSeriesJpaEntity entity = buildEntity(series.getId());
		final DocumentSeriesJpaEntity saved = buildEntity(series.getId());
		when(mapper.map(series)).thenReturn(entity);
		when(repository.existsById(series.getId())).thenReturn(false);
		when(repository.save(entity)).thenReturn(saved);
		when(mapper.map(saved)).thenReturn(series);

		final DocumentSeries result = adapter.save(series);

		assertThat(result).isSameAs(series);
		assertThat(entity.isNew()).isTrue();
		verify(repository).save(entity);
	}

	@Test
	@DisplayName("Saves an existing document series marking its entity as not new")
	void shouldSaveExistingDocumentSeriesAsNotNew() {
		final CompanyId companyId = CompanyId.of(UUID.randomUUID());
		final DocumentSeries series = DocumentSeries.placeholder(companyId, FiscalDocumentType.NFSE).reconfigure("001", 10L);
		final DocumentSeriesJpaEntity entity = buildEntity(series.getId());
		when(mapper.map(series)).thenReturn(entity);
		when(repository.existsById(series.getId())).thenReturn(true);
		when(repository.save(entity)).thenReturn(entity);
		when(mapper.map(entity)).thenReturn(series);

		adapter.save(series);

		assertThat(entity.isNew()).isFalse();
	}

	@Test
	@DisplayName("Finds a document series by company and document type")
	void shouldFindByCompanyAndDocumentType() {
		final CompanyId companyId = CompanyId.of(UUID.randomUUID());
		final DocumentSeries series = DocumentSeries.placeholder(companyId, FiscalDocumentType.NFE);
		final DocumentSeriesJpaEntity entity = buildEntity(series.getId());
		when(repository.findByCompanyIdAndDocumentType(companyId.value(), FiscalDocumentType.NFE))
				.thenReturn(Optional.of(entity));
		when(mapper.map(entity)).thenReturn(series);

		final Optional<DocumentSeries> result = adapter.findByCompanyIdAndDocumentType(companyId, FiscalDocumentType.NFE);

		assertThat(result).contains(series);
		verify(repository).findByCompanyIdAndDocumentType(companyId.value(), FiscalDocumentType.NFE);
	}

	@Test
	@DisplayName("Returns empty when no series is configured for the document type")
	void shouldReturnEmptyWhenNoSeriesConfiguredForType() {
		final CompanyId companyId = CompanyId.of(UUID.randomUUID());
		when(repository.findByCompanyIdAndDocumentType(companyId.value(), FiscalDocumentType.NFCE))
				.thenReturn(Optional.empty());

		assertThat(adapter.findByCompanyIdAndDocumentType(companyId, FiscalDocumentType.NFCE)).isEmpty();
	}

	private DocumentSeriesJpaEntity buildEntity(final UUID id) {
		return DocumentSeriesJpaEntity.builder().id(id).build();
	}
}
