package br.gravita.tax.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.IbgeMunicipalityDomain;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.shared.PersonType;
import br.gravita.core.domain.system.User;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.domain.tax.NfseDocument;
import br.gravita.core.domain.tax.NfseId;
import br.gravita.core.domain.tax.NfseTomador;
import br.gravita.core.domain.tax.PlaceOfProvision;
import br.gravita.core.domain.tax.ServiceCode;
import br.gravita.core.ports.inbound.tax.NfseListItem;
import br.gravita.core.ports.outbound.persistence.IbgeMunicipalityRepositoryPort;
import br.gravita.core.ports.outbound.persistence.system.UserRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.NfseRepositoryPort;
import br.gravita.core.usercases.CallerCompanyResolver;
import br.gravita.core.usercases.tax.ListNfseService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListNfseServiceTest {

	private static final String SP = "3550308";

	@Mock
	private NfseRepositoryPort nfseRepositoryPort;
	@Mock
	private IbgeMunicipalityRepositoryPort ibgeMunicipalityRepositoryPort;
	@Mock
	private UserRepositoryPort userRepositoryPort;

	private ListNfseService service;
	private final UserId callerId = UserId.generate();
	private final UUID companyId = UUID.randomUUID();

	@BeforeEach
	void setUp() {
		service = new ListNfseService(nfseRepositoryPort, ibgeMunicipalityRepositoryPort, new CallerCompanyResolver(userRepositoryPort));
	}

	@Test
	@DisplayName("Lists only the caller's company documents, with the municipality name and state resolved once per code")
	void listsTheCallersDocumentsWithMunicipality() {
		when(userRepositoryPort.findById(callerId)).thenReturn(Optional.of(User.builder().companyId(companyId).build()));
		when(nfseRepositoryPort.findAllByProvider(CompanyId.of(companyId))).thenReturn(List.of(rps(), rps()));
		when(ibgeMunicipalityRepositoryPort.findByIbgeCode(SP)).thenReturn(Optional.of(
				IbgeMunicipalityDomain.builder().ibgeCode(SP).name("São Paulo").stateCode("SP").build()));

		final List<NfseListItem> result = service.execute(new Context().withCaller(callerId));

		assertThat(result).hasSize(2);
		assertThat(result.get(0).municipalityName()).isEqualTo("São Paulo");
		assertThat(result.get(0).municipalityState()).isEqualTo("SP");
		verify(ibgeMunicipalityRepositoryPort, times(1)).findByIbgeCode(SP);
	}

	@Test
	@DisplayName("Keeps the document when the municipality is unknown")
	void keepsTheDocumentWhenTheMunicipalityIsUnknown() {
		when(userRepositoryPort.findById(callerId)).thenReturn(Optional.of(User.builder().companyId(companyId).build()));
		when(nfseRepositoryPort.findAllByProvider(CompanyId.of(companyId))).thenReturn(List.of(rps()));
		when(ibgeMunicipalityRepositoryPort.findByIbgeCode(SP)).thenReturn(Optional.empty());

		final List<NfseListItem> result = service.execute(new Context().withCaller(callerId));

		assertThat(result).singleElement().satisfies(item -> {
			assertThat(item.municipalityName()).isNull();
			assertThat(item.document().getServiceCode()).isNotBlank();
		});
	}

	@Test
	@DisplayName("Returns nothing when the caller has no company")
	void returnsNothingWithoutCompany() {
		when(userRepositoryPort.findById(callerId)).thenReturn(Optional.of(User.builder().build()));

		assertThat(service.execute(new Context().withCaller(callerId))).isEmpty();
		verify(nfseRepositoryPort, never()).findAllByProvider(org.mockito.ArgumentMatchers.any());
	}

	private NfseDocument rps() {
		return NfseDocument.issueRps()
				.id(NfseId.of(UUID.randomUUID()))
				.providerCompanyId(CompanyId.of(companyId))
				.providerMunicipalityIbgeCode(SP)
				.tomador(NfseTomador.of(null, "52998224725", PersonType.INDIVIDUAL, "Pessoa Fisica", null, null))
				.serviceCode(ServiceCode.of("1.05"))
				.placeOfProvision(PlaceOfProvision.PROVIDER)
				.issMunicipalityIbgeCode(SP)
				.serviceAmount(new BigDecimal("1000.00"))
				.issRate(new BigDecimal("5.0000"))
				.issAmount(new BigDecimal("50.00"))
				.withholdings(List.of())
				.discrimination("Consultoria")
				.rpsSeries("RPS")
				.rpsNumber(1L)
				.createdAt(Instant.now())
				.build();
	}
}
