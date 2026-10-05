package br.gravita.adapters.outbound.persistence.adapters.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.NfseJpaEntity;
import br.gravita.adapters.outbound.persistence.entities.tax.NfseNumberSequenceJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.tax.NfsePersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.tax.NfseJpaRepository;
import br.gravita.adapters.outbound.persistence.repositories.tax.NfseNumberSequenceJpaRepository;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.shared.PersonType;
import br.gravita.core.domain.tax.NfseDocument;
import br.gravita.core.domain.tax.NfseId;
import br.gravita.core.domain.tax.NfseNumber;
import br.gravita.core.domain.tax.NfseStatus;
import br.gravita.core.domain.tax.NfseTomador;
import br.gravita.core.domain.tax.PlaceOfProvision;
import br.gravita.core.domain.tax.ServiceCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NfseRepositoryAdapterTest {

	private static final Instant FROM = Instant.parse("2026-10-01T00:00:00Z");
	private static final Instant TO = Instant.parse("2026-11-01T00:00:00Z");

	@Mock
	private NfseJpaRepository repository;

	@Mock
	private NfseNumberSequenceJpaRepository sequenceRepository;

	@Mock
	private NfsePersistenceMapper mapper;

	@InjectMocks
	private NfseRepositoryAdapter adapter;

	@Test
	@DisplayName("Saves a new NFS-e marking its entity as new")
	void shouldSaveNewNfse() {
		final NfseDocument document = buildRps();
		final NfseJpaEntity entity = buildEntity(document.getId());
		final NfseJpaEntity saved = buildEntity(document.getId());
		when(mapper.map(document)).thenReturn(entity);
		when(repository.existsById(document.getId().value())).thenReturn(false);
		when(repository.save(entity)).thenReturn(saved);
		when(mapper.map(saved)).thenReturn(document);

		final NfseDocument result = adapter.save(document);

		assertThat(result).isSameAs(document);
		assertThat(entity.isNew()).isTrue();
		verify(repository).save(entity);
	}

	@Test
	@DisplayName("Saves an existing NFS-e marking its entity as not new")
	void shouldSaveExistingNfseAsNotNew() {
		final NfseDocument document = buildRps();
		final NfseJpaEntity entity = buildEntity(document.getId());
		when(mapper.map(document)).thenReturn(entity);
		when(repository.existsById(document.getId().value())).thenReturn(true);
		when(repository.save(entity)).thenReturn(entity);
		when(mapper.map(entity)).thenReturn(document);

		adapter.save(document);

		assertThat(entity.isNew()).isFalse();
	}

	@Test
	@DisplayName("Finds an NFS-e by id")
	void shouldFindNfseById() {
		final NfseDocument document = buildRps();
		final NfseJpaEntity entity = buildEntity(document.getId());
		when(repository.findById(document.getId().value())).thenReturn(Optional.of(entity));
		when(mapper.map(entity)).thenReturn(document);

		final Optional<NfseDocument> result = adapter.findById(document.getId());

		assertThat(result).contains(document);
		verify(repository).findById(document.getId().value());
	}

	@Test
	@DisplayName("Finds an NFS-e by id for update")
	void shouldFindNfseByIdForUpdate() {
		final NfseDocument document = buildRps();
		final NfseJpaEntity entity = buildEntity(document.getId());
		when(repository.findByIdForUpdate(document.getId().value())).thenReturn(Optional.of(entity));
		when(mapper.map(entity)).thenReturn(document);

		final Optional<NfseDocument> result = adapter.findByIdForUpdate(document.getId());

		assertThat(result).contains(document);
		verify(repository).findByIdForUpdate(document.getId().value());
	}

	@Test
	@DisplayName("Returns empty when no NFS-e exists for the id to update")
	void shouldReturnEmptyWhenNoNfseExistsForTheIdToUpdate() {
		final NfseId id = NfseId.of(UUID.randomUUID());
		when(repository.findByIdForUpdate(id.value())).thenReturn(Optional.empty());

		assertThat(adapter.findByIdForUpdate(id)).isEmpty();
	}

	@Test
	@DisplayName("Starts the numbering at 1 with the default series for a new company and municipality")
	void shouldStartNumberingAtOneForANewCompanyAndMunicipality() {
		final CompanyId companyId = CompanyId.of(UUID.randomUUID());
		when(sequenceRepository.findByCompanyIdAndMunicipalityIbge(companyId.value(), "3550308"))
				.thenReturn(Optional.empty());

		final NfseNumber allocated = adapter.allocateNextNumber(companyId, "3550308");

		assertThat(allocated).isEqualTo(new NfseNumber("1", 1L));
		final ArgumentCaptor<NfseNumberSequenceJpaEntity> captor =
				ArgumentCaptor.forClass(NfseNumberSequenceJpaEntity.class);
		verify(sequenceRepository).save(captor.capture());
		assertThat(captor.getValue().getCompanyId()).isEqualTo(companyId.value());
		assertThat(captor.getValue().getMunicipalityIbge()).isEqualTo("3550308");
		assertThat(captor.getValue().getNextNumber()).isEqualTo(2L);
	}

	@Test
	@DisplayName("Allocates the next number of an existing sequence and advances it")
	void shouldAllocateTheNextNumberOfAnExistingSequenceAndAdvanceIt() {
		final CompanyId companyId = CompanyId.of(UUID.randomUUID());
		final NfseNumberSequenceJpaEntity sequence = NfseNumberSequenceJpaEntity.builder()
				.id(UUID.randomUUID())
				.companyId(companyId.value())
				.municipalityIbge("3550308")
				.series("1")
				.nextNumber(7L)
				.build();
		when(sequenceRepository.findByCompanyIdAndMunicipalityIbge(companyId.value(), "3550308"))
				.thenReturn(Optional.of(sequence));

		final NfseNumber allocated = adapter.allocateNextNumber(companyId, "3550308");

		assertThat(allocated).isEqualTo(new NfseNumber("1", 7L));
		assertThat(sequence.getNextNumber()).isEqualTo(8L);
		verify(sequenceRepository).save(same(sequence));
	}

	@Test
	@DisplayName("Finds the NFS-e authorized within the window")
	void shouldFindAuthorizedBetween() {
		final NfseDocument document = buildRps();
		final NfseJpaEntity entity = buildEntity(document.getId());
		when(repository.findByStatusAndAuthorizedAtGreaterThanEqualAndAuthorizedAtLessThanOrderByAuthorizedAt(
				NfseStatus.AUTHORIZED, FROM, TO)).thenReturn(List.of(entity));
		when(mapper.map(entity)).thenReturn(document);

		final List<NfseDocument> result = adapter.findAuthorizedBetween(FROM, TO);

		assertThat(result).containsExactly(document);
		verify(repository).findByStatusAndAuthorizedAtGreaterThanEqualAndAuthorizedAtLessThanOrderByAuthorizedAt(
				NfseStatus.AUTHORIZED, FROM, TO);
	}

	private NfseJpaEntity buildEntity(final NfseId id) {
		return NfseJpaEntity.builder().id(id.value()).build();
	}

	private NfseDocument buildRps() {
		final NfseTomador tomador = NfseTomador.of(null, "52998224725", PersonType.INDIVIDUAL, "Pessoa Fisica", null,
				null);
		return NfseDocument.issueRps()
				.id(NfseId.of(UUID.randomUUID()))
				.providerCompanyId(CompanyId.of(UUID.randomUUID()))
				.providerMunicipalityIbgeCode("3550308")
				.tomador(tomador)
				.serviceCode(ServiceCode.of("1.05"))
				.placeOfProvision(PlaceOfProvision.RECIPIENT)
				.issMunicipalityIbgeCode("3304557")
				.serviceAmount(new BigDecimal("1000.00"))
				.issRate(new BigDecimal("5.0000"))
				.issAmount(new BigDecimal("50.00"))
				.issRateOverrideJustification(null)
				.withholdings(List.of())
				.discrimination("Desenvolvimento de software sob demanda")
				.rpsSeries("RPS1")
				.rpsNumber(42L)
				.createdAt(Instant.parse("2026-10-01T10:00:00Z"))
				.build();
	}
}
