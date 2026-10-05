package br.gravita.tax.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gravita.adapters.outbound.persistence.entities.tax.NfseJpaEntity;
import br.gravita.adapters.outbound.persistence.repositories.tax.NfseJpaRepository;
import br.gravita.core.domain.masterdata.Company;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.DocumentSeries;
import br.gravita.core.domain.masterdata.FiscalDocumentType;
import br.gravita.core.domain.masterdata.SefazEnvironment;
import br.gravita.core.domain.masterdata.TaxRegime;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.domain.shared.PersonType;
import br.gravita.core.domain.tax.NfseDocument;
import br.gravita.core.domain.tax.NfseId;
import br.gravita.core.domain.tax.NfseStatus;
import br.gravita.core.domain.tax.NfseTomador;
import br.gravita.core.domain.tax.PlaceOfProvision;
import br.gravita.core.domain.tax.ServiceCode;
import br.gravita.core.ports.outbound.persistence.CompanyRepositoryPort;
import br.gravita.core.ports.outbound.persistence.DocumentSeriesRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.NfseRepositoryPort;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class ConvertRpsToNfseEndToEndTest {

	private static final String SP = "3550308";
	private static final String RIO = "3304557";

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private CompanyRepositoryPort companyRepositoryPort;
	@Autowired
	private DocumentSeriesRepositoryPort documentSeriesRepositoryPort;
	@Autowired
	private NfseRepositoryPort nfseRepositoryPort;
	@Autowired
	private NfseJpaRepository nfseJpaRepository;

	private CompanyId companyId;
	private long rpsCounter;

	@BeforeEach
	void seed() {
		companyId = CompanyId.of(UUID.randomUUID());
		companyRepositoryPort.save(Company.of(companyId, "Acme Ltda", Document.cnpj("11222333000181"), "123456789", "987654",
				"6201500", TaxRegime.LUCRO_PRESUMIDO, false, SefazEnvironment.HOMOLOGATION, "Rua Teste, 100", "SP",
				"nfse@example.com", "11999999999", null, null));
		documentSeriesRepositoryPort
				.save(DocumentSeries.placeholder(companyId, FiscalDocumentType.NFE).reconfigure("001", 500L));
	}

	private UUID issueRps(String municipality) {
		NfseDocument rps = NfseDocument.issueRps(NfseId.of(UUID.randomUUID()), companyId, municipality,
				NfseTomador.of(null, "52998224725", PersonType.INDIVIDUAL, "Pessoa Fisica", null, null),
				ServiceCode.of("1.05"), PlaceOfProvision.PROVIDER, municipality, new BigDecimal("1000.00"),
				new BigDecimal("5.0000"), new BigDecimal("50.00"), null, List.of(), "Consultoria", "RPS",
				++rpsCounter, Instant.now());
		return nfseRepositoryPort.save(rps).getId().value();
	}

	private String body(UUID... ids) {
		StringBuilder sb = new StringBuilder("{\"rpsIds\": [");
		for (int i = 0; i < ids.length; i++) {
			sb.append(i == 0 ? "" : ",").append('"').append(ids[i]).append('"');
		}
		return sb.append("]}").toString();
	}

	private NfseJpaEntity persisted(UUID id) {
		return nfseJpaRepository.findById(id).orElseThrow();
	}

	@Test
	@DisplayName("Converts a single RPS into an NFS-e draft without transmitting it")
	void convertsASingleRpsIntoADraftWithoutTransmittingIt() throws Exception {
		UUID rps = issueRps(SP);

		mockMvc.perform(post("/api/nfse/rps/convert").contentType(MediaType.APPLICATION_JSON).content(body(rps)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.ids[0]").value(rps.toString()));

		NfseJpaEntity converted = persisted(rps);
		assertThat(converted.getStatus()).isEqualTo(NfseStatus.DRAFT);
		assertThat(converted.getNfseSeries()).isEqualTo("1");
		assertThat(converted.getNfseNumber()).isEqualTo(1L);
		assertThat(converted.getDraftAt()).isNotNull();
		assertThat(converted.getRpsNumber()).isEqualTo(1L);
	}

	@Test
	@DisplayName("Numbers a batch consecutively per municipality without touching the NF-e series")
	void aBatchGetsConsecutiveNumbersPerMunicipalityAndLeavesTheNfeSeriesUntouched() throws Exception {
		UUID sp1 = issueRps(SP);
		UUID rio1 = issueRps(RIO);
		UUID sp2 = issueRps(SP);

		mockMvc.perform(post("/api/nfse/rps/convert").contentType(MediaType.APPLICATION_JSON)
						.content(body(sp1, rio1, sp2)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.ids.length()").value(3))
				.andExpect(jsonPath("$.ids[0]").value(sp1.toString()))
				.andExpect(jsonPath("$.ids[1]").value(rio1.toString()))
				.andExpect(jsonPath("$.ids[2]").value(sp2.toString()));

		assertThat(List.of(sp1, sp2)).extracting(id -> persisted(id).getNfseNumber())
				.containsExactlyInAnyOrder(1L, 2L);
		assertThat(persisted(rio1).getNfseNumber()).isEqualTo(1L);
		assertThat(documentSeriesRepositoryPort.findByCompanyIdAndDocumentType(companyId, FiscalDocumentType.NFE)
				.orElseThrow().getNextNumber()).isEqualTo(500L);
	}

	@Test
	@DisplayName("Converting the same RPS twice creates no second document and consumes no extra number")
	void convertingTheSameRpsTwiceDoesNotCreateASecondDocumentOrConsumeANumber() throws Exception {
		UUID rps = issueRps(SP);
		UUID next = issueRps(SP);

		for (int i = 0; i < 2; i++) {
			mockMvc.perform(post("/api/nfse/rps/convert").contentType(MediaType.APPLICATION_JSON)
							.content(body(rps)))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.ids[0]").value(rps.toString()));
		}
		mockMvc.perform(post("/api/nfse/rps/convert").contentType(MediaType.APPLICATION_JSON).content(body(next)))
				.andExpect(status().isOk());

		assertThat(nfseJpaRepository.count()).isEqualTo(2);
		assertThat(persisted(rps).getNfseNumber()).isEqualTo(1L);
		assertThat(persisted(next).getNfseNumber()).isEqualTo(2L);
	}

	@Test
	@DisplayName("Returns 404 for an unknown RPS and 400 for an empty or missing RPS list")
	void anUnknownRpsIs404AndAnEmptyOrMissingListIs400() throws Exception {
		mockMvc.perform(post("/api/nfse/rps/convert").contentType(MediaType.APPLICATION_JSON)
						.content(body(UUID.randomUUID())))
				.andExpect(status().isNotFound());
		mockMvc.perform(post("/api/nfse/rps/convert").contentType(MediaType.APPLICATION_JSON)
						.content("{\"rpsIds\": []}"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(post("/api/nfse/rps/convert").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest());
	}
}
