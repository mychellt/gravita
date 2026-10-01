package br.gravita.tax.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.shared.PersonType;
import br.gravita.core.domain.tax.NfseDocument;
import br.gravita.core.domain.tax.NfseId;
import br.gravita.core.domain.tax.NfseNumber;
import br.gravita.core.domain.tax.NfseStatus;
import br.gravita.core.domain.tax.NfseTomador;
import br.gravita.core.domain.tax.PlaceOfProvision;
import br.gravita.core.domain.tax.RpsId;
import br.gravita.core.domain.tax.ServiceCode;
import br.gravita.core.ports.inbound.tax.ConvertRpsToNfseCommand;
import br.gravita.core.ports.outbound.persistence.tax.NfseRepositoryPort;
import br.gravita.core.usercases.tax.ConvertRpsToNfseService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConvertRpsToNfseServiceTest {

	private static final String SP = "3550308";

	@Mock
	private NfseRepositoryPort nfseRepositoryPort;

	private ConvertRpsToNfseService service;
	private final CompanyId companyId = CompanyId.of(UUID.randomUUID());

	@BeforeEach
	void setUp() {
		service = new ConvertRpsToNfseService(nfseRepositoryPort);
	}

	private NfseDocument rps() {
		return NfseDocument.issueRps(NfseId.of(UUID.randomUUID()), companyId, SP,
				NfseTomador.of(null, "52998224725", PersonType.INDIVIDUAL, "Pessoa Fisica", null, null),
				ServiceCode.of("1.05"), PlaceOfProvision.PROVIDER, SP, new BigDecimal("1000.00"),
				new BigDecimal("5.0000"), new BigDecimal("50.00"), null, List.of(), "Consultoria", "RPS", 1L,
				Instant.now());
	}

	private void stored(NfseDocument document) {
		when(nfseRepositoryPort.findByIdForUpdate(document.getId())).thenReturn(Optional.of(document));
	}

	private static ConvertRpsToNfseCommand commandFor(NfseDocument... documents) {
		return new ConvertRpsToNfseCommand(java.util.Arrays.stream(documents).map(NfseDocument::getRpsId).toList());
	}

	@Test
	@DisplayName("Converts a single RPS into a draft with the next number of its company and municipality")
	void ac1_convertsASingleRpsIntoADraftWithTheNextNumberOfItsCompanyAndMunicipality() {
		NfseDocument rps = rps();
		stored(rps);
		when(nfseRepositoryPort.allocateNextNumber(companyId, SP)).thenReturn(new NfseNumber("1", 15L));

		List<NfseId> ids = service.execute(commandFor(rps));

		assertThat(ids).containsExactly(rps.getId());
		ArgumentCaptor<NfseDocument> saved = ArgumentCaptor.forClass(NfseDocument.class);
		verify(nfseRepositoryPort).save(saved.capture());
		assertThat(saved.getValue().getId()).isEqualTo(rps.getId());
		assertThat(saved.getValue().getStatus()).isEqualTo(NfseStatus.DRAFT);
		assertThat(saved.getValue().getNfseSeries()).isEqualTo("1");
		assertThat(saved.getValue().getNfseNumber()).isEqualTo(15L);
		assertThat(saved.getValue().getDraftAt()).isNotNull();
	}

	@Test
	@DisplayName("Converts every RPS of a batch, returning ids in the given order with consecutive numbers")
	void ac1and2_aBatchConvertsEveryRpsReturningTheIdsInTheOrderGivenWithConsecutiveNumbers() {
		NfseDocument first = rps();
		NfseDocument second = rps();
		NfseDocument third = rps();
		stored(first);
		stored(second);
		stored(third);
		when(nfseRepositoryPort.allocateNextNumber(companyId, SP)).thenReturn(new NfseNumber("1", 1L),
				new NfseNumber("1", 2L), new NfseNumber("1", 3L));

		List<NfseId> ids = service.execute(commandFor(third, first, second));

		assertThat(ids).containsExactly(third.getId(), first.getId(), second.getId());
		ArgumentCaptor<NfseDocument> saved = ArgumentCaptor.forClass(NfseDocument.class);
		verify(nfseRepositoryPort, org.mockito.Mockito.times(3)).save(saved.capture());
		assertThat(saved.getAllValues()).extracting(NfseDocument::getStatus).containsOnly(NfseStatus.DRAFT);
		assertThat(saved.getAllValues()).extracting(NfseDocument::getNfseNumber).containsExactlyInAnyOrder(1L, 2L, 3L);
	}

	@Test
	@DisplayName("Numbers each document within the scope of its own company and provider municipality")
	void ac2_eachDocumentIsNumberedInTheScopeOfItsOwnCompanyAndProviderMunicipality() {
		CompanyId other = CompanyId.of(UUID.randomUUID());
		NfseDocument mine = rps();
		NfseDocument theirs = NfseDocument.issueRps(NfseId.of(UUID.randomUUID()), other, "3304557",
				mine.getTomador(), ServiceCode.of("1.05"), PlaceOfProvision.PROVIDER, "3304557",
				new BigDecimal("100.00"), new BigDecimal("5.0000"), new BigDecimal("5.00"), null, List.of(), "x",
				"RPS", 1L, Instant.now());
		stored(mine);
		stored(theirs);
		when(nfseRepositoryPort.allocateNextNumber(companyId, SP)).thenReturn(new NfseNumber("1", 4L));
		when(nfseRepositoryPort.allocateNextNumber(other, "3304557")).thenReturn(new NfseNumber("A", 70L));

		service.execute(commandFor(mine, theirs));

		verify(nfseRepositoryPort).allocateNextNumber(companyId, SP);
		verify(nfseRepositoryPort).allocateNextNumber(other, "3304557");
	}

	@Test
	@DisplayName("Returns the same id for an already converted RPS without a new number or second document")
	void ac4_anAlreadyConvertedRpsYieldsTheSameIdWithoutANewNumberOrSecondDocument() {
		NfseDocument converted = rps().convertToNfse("1", 3L, Instant.now());
		stored(converted);

		List<NfseId> ids = service.execute(commandFor(converted));

		assertThat(ids).containsExactly(converted.getId());
		verify(nfseRepositoryPort, never()).allocateNextNumber(any(), any());
		verify(nfseRepositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("Converts an id repeated within one batch only once")
	void ac4_theSameIdRepeatedInOneBatchIsConvertedOnce() {
		NfseDocument rps = rps();
		stored(rps);
		when(nfseRepositoryPort.allocateNextNumber(companyId, SP)).thenReturn(new NfseNumber("1", 1L));

		List<NfseId> ids = service.execute(commandFor(rps, rps));

		assertThat(ids).containsExactly(rps.getId());
		verify(nfseRepositoryPort).save(any());
	}

	@Test
	@DisplayName("Reports not found for an unknown RPS")
	void anUnknownRpsIsNotFound() {
		RpsId missing = RpsId.of(UUID.randomUUID());
		when(nfseRepositoryPort.findByIdForUpdate(missing.toNfseId())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(new ConvertRpsToNfseCommand(List.of(missing))))
				.isInstanceOf(ResourceNotFoundException.class);
		verify(nfseRepositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("Requires at least one RPS id in the command")
	void theCommandNeedsAtLeastOneRpsId() {
		assertThatThrownBy(() -> new ConvertRpsToNfseCommand(List.of())).isInstanceOf(BusinessRuleException.class);
		assertThatThrownBy(() -> new ConvertRpsToNfseCommand(null)).isInstanceOf(BusinessRuleException.class);
	}
}
