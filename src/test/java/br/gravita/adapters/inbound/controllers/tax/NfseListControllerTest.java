package br.gravita.adapters.inbound.controllers.tax;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gravita.adapters.configuration.web.WebMvcConfiguration;
import br.gravita.adapters.inbound.controllers.tax.controllers.NfseController;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.shared.PersonType;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.domain.tax.NfseDocument;
import br.gravita.core.domain.tax.NfseId;
import br.gravita.core.domain.tax.NfseTomador;
import br.gravita.core.domain.tax.PlaceOfProvision;
import br.gravita.core.domain.tax.ServiceCode;
import br.gravita.core.ports.inbound.tax.CancelNfseUseCase;
import br.gravita.core.ports.inbound.tax.ConvertRpsToNfseUseCase;
import br.gravita.core.ports.inbound.tax.IssueRpsUseCase;
import br.gravita.core.ports.inbound.tax.ListNfseUseCase;
import br.gravita.core.ports.inbound.tax.NfseListItem;
import br.gravita.core.ports.inbound.tax.TransmitNfseUseCase;
import br.gravita.core.ports.outbound.security.SessionStorePort;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(NfseController.class)
@Import(WebMvcConfiguration.class)
class NfseListControllerTest {

	private static final String BEARER = "Bearer session-token";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ListNfseUseCase listNfseUseCase;
	@MockitoBean
	private IssueRpsUseCase issueRpsUseCase;
	@MockitoBean
	private ConvertRpsToNfseUseCase convertRpsToNfseUseCase;
	@MockitoBean
	private TransmitNfseUseCase transmitNfseUseCase;
	@MockitoBean
	private CancelNfseUseCase cancelNfseUseCase;
	@MockitoBean
	private SessionStorePort sessionStorePort;

	@BeforeEach
	void authenticate() {
		when(sessionStorePort.resolve("session-token")).thenReturn(Optional.of(UserId.generate()));
	}

	@Test
	@DisplayName("GET /api/nfse lists the caller's RPS/NFSe with tomador, municipality, amounts and status")
	void shouldListNfse() throws Exception {
		final NfseDocument rps = NfseDocument.issueRps()
				.id(NfseId.of(UUID.randomUUID()))
				.providerCompanyId(CompanyId.of(UUID.randomUUID()))
				.providerMunicipalityIbgeCode("3550308")
				.tomador(NfseTomador.of(null, "52998224725", PersonType.INDIVIDUAL, "Pessoa Fisica", null, null))
				.serviceCode(ServiceCode.of("1.05"))
				.placeOfProvision(PlaceOfProvision.PROVIDER)
				.issMunicipalityIbgeCode("3550308")
				.serviceAmount(new BigDecimal("1000.00"))
				.issRate(new BigDecimal("5.0000"))
				.issAmount(new BigDecimal("50.00"))
				.withholdings(List.of())
				.discrimination("Consultoria")
				.rpsSeries("RPS")
				.rpsNumber(7L)
				.createdAt(Instant.parse("2026-10-01T12:00:00Z"))
				.build();
		when(listNfseUseCase.execute(any())).thenReturn(List.of(new NfseListItem(rps, "São Paulo", "SP")));

		mockMvc.perform(get("/api/nfse").header("Authorization", BEARER))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].status").value("RPS"))
				.andExpect(jsonPath("$[0].rpsNumber").value(7))
				.andExpect(jsonPath("$[0].tomadorName").value("Pessoa Fisica"))
				.andExpect(jsonPath("$[0].municipalityName").value("São Paulo"))
				.andExpect(jsonPath("$[0].municipalityState").value("SP"))
				.andExpect(jsonPath("$[0].serviceAmount").value(1000.00))
				.andExpect(jsonPath("$[0].issAmount").value(50.00));
	}
}
