package br.gravita.adapters.outbound.persistence.adapters.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.InboundNfeJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.tax.InboundNfePersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.tax.InboundNfeJpaRepository;
import br.gravita.core.domain.exceptions.DuplicateResourceException;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.domain.tax.InboundNfe;
import br.gravita.core.domain.tax.InboundNfeId;
import br.gravita.core.domain.tax.InboundNfeItem;
import br.gravita.core.domain.tax.InboundNfeStatus;
import br.gravita.core.domain.tax.InboundNfeTotals;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InboundNfeRepositoryAdapterTest {

	private static final String ACCESS_KEY = "35240111222333000181550010000012345123456789";
	private static final Instant FROM = Instant.parse("2028-02-01T00:00:00Z");
	private static final Instant TO = Instant.parse("2028-03-01T00:00:00Z");

	@Mock
	private InboundNfeJpaRepository repository;

	@Mock
	private InboundNfePersistenceMapper mapper;

	@InjectMocks
	private InboundNfeRepositoryAdapter adapter;

	@Test
	@DisplayName("Saves a new inbound NF-e marking its entity as new")
	void shouldSaveNewInboundNfe() {
		final InboundNfe inboundNfe = buildInboundNfe(CompanyId.of(UUID.randomUUID()));
		final InboundNfeJpaEntity entity = buildEntity(inboundNfe.getId());
		final InboundNfeJpaEntity saved = buildEntity(inboundNfe.getId());
		when(mapper.map(inboundNfe)).thenReturn(entity);
		when(repository.existsById(inboundNfe.getId().value())).thenReturn(false);
		when(repository.saveAndFlush(entity)).thenReturn(saved);
		when(mapper.map(saved)).thenReturn(inboundNfe);

		final InboundNfe result = adapter.save(inboundNfe);

		assertThat(result).isSameAs(inboundNfe);
		assertThat(entity.isNew()).isTrue();
		verify(repository).saveAndFlush(entity);
	}

	@Test
	@DisplayName("Throws DuplicateResourceException when a second inbound NF-e reuses an access key")
	void savingASecondInboundNfeWithAnAlreadyUsedAccessKeyThrowsADuplicateResourceException() {
		final InboundNfe inboundNfe = buildInboundNfe(CompanyId.of(UUID.randomUUID()));
		final InboundNfeJpaEntity entity = buildEntity(inboundNfe.getId());
		entity.setAccessKey(ACCESS_KEY);
		when(mapper.map(inboundNfe)).thenReturn(entity);
		when(repository.existsById(inboundNfe.getId().value())).thenReturn(false);
		when(repository.saveAndFlush(entity)).thenThrow(new DataIntegrityViolationException("duplicate",
				new IllegalStateException("Unique index violation on column access_key")));

		assertThatThrownBy(() -> adapter.save(inboundNfe))
				.isInstanceOf(DuplicateResourceException.class)
				.hasMessageContaining(ACCESS_KEY);
	}

	@Test
	@DisplayName("Rethrows an integrity violation that is not about the access key")
	void rethrowsAnIntegrityViolationThatIsNotAboutTheAccessKey() {
		final InboundNfe inboundNfe = buildInboundNfe(CompanyId.of(UUID.randomUUID()));
		final InboundNfeJpaEntity entity = buildEntity(inboundNfe.getId());
		final DataIntegrityViolationException violation = new DataIntegrityViolationException("other",
				new IllegalStateException("Not null violation on column supplier_name"));
		when(mapper.map(inboundNfe)).thenReturn(entity);
		when(repository.existsById(inboundNfe.getId().value())).thenReturn(false);
		when(repository.saveAndFlush(entity)).thenThrow(violation);

		assertThatThrownBy(() -> adapter.save(inboundNfe)).isSameAs(violation);
	}

	@Test
	@DisplayName("Finds an inbound NF-e by id")
	void shouldFindInboundNfeById() {
		final InboundNfe inboundNfe = buildInboundNfe(CompanyId.of(UUID.randomUUID()));
		final InboundNfeJpaEntity entity = buildEntity(inboundNfe.getId());
		when(repository.findById(inboundNfe.getId().value())).thenReturn(Optional.of(entity));
		when(mapper.map(entity)).thenReturn(inboundNfe);

		final Optional<InboundNfe> result = adapter.findById(inboundNfe.getId());

		assertThat(result).contains(inboundNfe);
		verify(repository).findById(inboundNfe.getId().value());
	}

	@Test
	@DisplayName("Finds an inbound NF-e by access key")
	void shouldFindInboundNfeByAccessKey() {
		final InboundNfe inboundNfe = buildInboundNfe(CompanyId.of(UUID.randomUUID()));
		final InboundNfeJpaEntity entity = buildEntity(inboundNfe.getId());
		when(repository.findByAccessKey(ACCESS_KEY)).thenReturn(Optional.of(entity));
		when(mapper.map(entity)).thenReturn(inboundNfe);

		final Optional<InboundNfe> result = adapter.findByAccessKey(ACCESS_KEY);

		assertThat(result).contains(inboundNfe);
		verify(repository).findByAccessKey(ACCESS_KEY);
	}

	@Test
	@DisplayName("Finds the NF-e issued within the window")
	void shouldFindIssuedBetween() {
		final InboundNfe inboundNfe = buildInboundNfe(CompanyId.of(UUID.randomUUID()));
		final InboundNfeJpaEntity entity = buildEntity(inboundNfe.getId());
		when(repository.findByIssuedAtGreaterThanEqualAndIssuedAtLessThanOrderByIssuedAt(FROM, TO))
				.thenReturn(List.of(entity));
		when(mapper.map(entity)).thenReturn(inboundNfe);

		final List<InboundNfe> result = adapter.findIssuedBetween(FROM, TO);

		assertThat(result).containsExactly(inboundNfe);
		verify(repository).findByIssuedAtGreaterThanEqualAndIssuedAtLessThanOrderByIssuedAt(FROM, TO);
	}

	@Test
	@DisplayName("Finds the company's NF-e issued within the window")
	void shouldFindIssuedByCompanyBetween() {
		final CompanyId companyId = CompanyId.of(UUID.randomUUID());
		final InboundNfe inboundNfe = buildInboundNfe(companyId);
		final InboundNfeJpaEntity entity = buildEntity(inboundNfe.getId());
		when(repository.findByCompanyIdAndIssuedAtGreaterThanEqualAndIssuedAtLessThanOrderByIssuedAt(companyId.value(),
				FROM, TO)).thenReturn(List.of(entity));
		when(mapper.map(entity)).thenReturn(inboundNfe);

		final List<InboundNfe> result = adapter.findIssuedByCompanyBetween(companyId, FROM, TO);

		assertThat(result).containsExactly(inboundNfe);
		verify(repository).findByCompanyIdAndIssuedAtGreaterThanEqualAndIssuedAtLessThanOrderByIssuedAt(
				companyId.value(), FROM, TO);
	}

	@Test
	@DisplayName("Finds only the company's confirmed NF-e issued within the window")
	void shouldFindConfirmedByCompanyBetween() {
		final CompanyId companyId = CompanyId.of(UUID.randomUUID());
		final InboundNfe inboundNfe = buildInboundNfe(companyId);
		final InboundNfeJpaEntity entity = buildEntity(inboundNfe.getId());
		when(repository.findByCompanyIdAndStatusAndIssuedAtGreaterThanEqualAndIssuedAtLessThanOrderByIssuedAt(
				companyId.value(), InboundNfeStatus.CONFIRMED, FROM, TO)).thenReturn(List.of(entity));
		when(mapper.map(entity)).thenReturn(inboundNfe);

		final List<InboundNfe> result = adapter.findConfirmedByCompanyBetween(companyId, FROM, TO);

		assertThat(result).containsExactly(inboundNfe);
		verify(repository).findByCompanyIdAndStatusAndIssuedAtGreaterThanEqualAndIssuedAtLessThanOrderByIssuedAt(
				companyId.value(), InboundNfeStatus.CONFIRMED, FROM, TO);
	}

	private InboundNfeJpaEntity buildEntity(final InboundNfeId id) {
		return InboundNfeJpaEntity.builder().id(id.value()).build();
	}

	private InboundNfe buildInboundNfe(final CompanyId companyId) {
		final BigDecimal total = new BigDecimal("10.00");
		final InboundNfeItem item = new InboundNfeItem("SKU", "Item", "73181500", "1102", "UN", BigDecimal.ONE, total,
				total, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
		return InboundNfe.importedFromXml(InboundNfeId.of(UUID.randomUUID()), companyId, ACCESS_KEY, "1", "1",
				Document.cnpj("11222333000181"), "Fornecedor", Instant.parse("2028-02-05T12:00:00Z"), List.of(item),
				new InboundNfeTotals(total, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
						BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, total),
				"xml-ref");
	}
}
