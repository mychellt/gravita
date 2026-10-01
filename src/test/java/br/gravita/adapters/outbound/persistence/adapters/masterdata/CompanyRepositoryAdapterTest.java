package br.gravita.adapters.outbound.persistence.adapters.masterdata;

import br.gravita.adapters.outbound.persistence.entities.masterdata.CompanyJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.masterdata.CompanyPersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.masterdata.CompanyJpaRepository;
import br.gravita.core.domain.masterdata.Company;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.SefazEnvironment;
import br.gravita.core.domain.masterdata.TaxRegime;
import br.gravita.core.domain.shared.Document;
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
class CompanyRepositoryAdapterTest {

	@Mock
	private CompanyJpaRepository repository;

	@Mock
	private CompanyPersistenceMapper mapper;

	@InjectMocks
	private CompanyRepositoryAdapter adapter;

	@Test
	@DisplayName("Persists a new company using the application-assigned id")
	void shouldPersistNewCompanyWithApplicationAssignedId() {
		final Company company = buildCompany();
		final CompanyJpaEntity entity = buildEntity(company.getId());
		final CompanyJpaEntity saved = buildEntity(company.getId());
		when(mapper.map(company)).thenReturn(entity);
		when(repository.existsById(company.getId().value())).thenReturn(false);
		when(repository.save(entity)).thenReturn(saved);
		when(mapper.map(saved)).thenReturn(company);

		final Company result = adapter.save(company);

		assertThat(result).isSameAs(company);
		assertThat(entity.isNew()).isTrue();
		verify(repository).save(entity);
	}

	@Test
	@DisplayName("Updates an existing company marking its entity as not new")
	void shouldUpdateExistingCompanyAsNotNew() {
		final Company company = buildCompany();
		final CompanyJpaEntity entity = buildEntity(company.getId());
		when(mapper.map(company)).thenReturn(entity);
		when(repository.existsById(company.getId().value())).thenReturn(true);
		when(repository.save(entity)).thenReturn(entity);
		when(mapper.map(entity)).thenReturn(company);

		adapter.save(company);

		assertThat(entity.isNew()).isFalse();
	}

	@Test
	@DisplayName("Finds a company by id")
	void shouldFindCompanyById() {
		final Company company = buildCompany();
		final CompanyJpaEntity entity = buildEntity(company.getId());
		when(repository.findById(company.getId().value())).thenReturn(Optional.of(entity));
		when(mapper.map(entity)).thenReturn(company);

		final Optional<Company> result = adapter.findById(company.getId());

		assertThat(result).contains(company);
		verify(repository).findById(company.getId().value());
	}

	@Test
	@DisplayName("Returns empty when the company does not exist")
	void shouldReturnEmptyWhenCompanyDoesNotExist() {
		final CompanyId id = CompanyId.of(UUID.randomUUID());
		when(repository.findById(id.value())).thenReturn(Optional.empty());

		assertThat(adapter.findById(id)).isEmpty();
	}

	private CompanyJpaEntity buildEntity(final CompanyId id) {
		return CompanyJpaEntity.builder().id(id.value()).build();
	}

	private Company buildCompany() {
		return Company.of(CompanyId.of(UUID.randomUUID()), Document.cnpj("11222333000181"), "123456789", "987654",
				"6201-5/01", TaxRegime.SIMPLES_NACIONAL, true, SefazEnvironment.HOMOLOGATION, "Rua Teste, 100", "SP",
				"fiscal@empresa.com", "11999999999", null, null);
	}
}
