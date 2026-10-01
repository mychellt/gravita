package br.gravita.tax.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gravita.adapters.outbound.persistence.repositories.tax.DiscriminationTemplateJpaRepository;
import br.gravita.core.domain.masterdata.Company;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.SefazEnvironment;
import br.gravita.core.domain.masterdata.TaxRegime;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.domain.shared.PersonRef;
import br.gravita.core.domain.shared.PersonType;
import br.gravita.core.domain.tax.NfseDocument;
import br.gravita.core.domain.tax.NfseId;
import br.gravita.core.domain.tax.NfseTomador;
import br.gravita.core.domain.tax.PlaceOfProvision;
import br.gravita.core.domain.tax.ServiceCode;
import br.gravita.core.ports.outbound.persistence.CompanyRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.NfseRepositoryPort;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
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
class ManageDiscriminationTemplateEndToEndTest {

	private static final String URL = "/api/nfse/discrimination-templates";

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private DiscriminationTemplateJpaRepository jpaRepository;
	@Autowired
	private CompanyRepositoryPort companyRepositoryPort;
	@Autowired
	private NfseRepositoryPort nfseRepositoryPort;

	private String create(String serviceCode, String text) throws Exception {
		String response = mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
						.content("{ \"serviceCode\": \"" + serviceCode + "\", \"templateText\": \"" + text + "\" }"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").exists())
				.andReturn().getResponse().getContentAsString();
		return JsonPath.read(response, "$.id");
	}

	@Test
	void ac1_createsUpdatesListsAndDeletes() throws Exception {
		String id = create("1.05", "Licenciamento de software");

		mockMvc.perform(get(URL)).andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].id").value(id))
				.andExpect(jsonPath("$[0].serviceCode").value("01.05"))
				.andExpect(jsonPath("$[0].templateText").value("Licenciamento de software"));

		mockMvc.perform(put(URL + "/" + id).contentType(MediaType.APPLICATION_JSON)
						.content("{ \"serviceCode\": \"08.01\", \"templateText\": \"Treinamento\" }"))
				.andExpect(status().isNoContent());
		mockMvc.perform(get(URL)).andExpect(jsonPath("$[0].serviceCode").value("08.01"))
				.andExpect(jsonPath("$[0].templateText").value("Treinamento"));

		mockMvc.perform(delete(URL + "/" + id)).andExpect(status().isNoContent());
		assertThat(jpaRepository.count()).isZero();
	}

	@Test
	void ac2_listingByServiceTypeReturnsOnlyThatType() throws Exception {
		create("01.05", "software a");
		create("1.05", "software b");
		create("08.01", "treinamento");

		mockMvc.perform(get(URL).param("serviceCode", "0105")).andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[?(@.serviceCode != '01.05')]").isEmpty());
		mockMvc.perform(get(URL).param("serviceCode", "08.01"))
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].templateText").value("treinamento"));
		mockMvc.perform(get(URL).param("serviceCode", "02.01"))
				.andExpect(jsonPath("$.length()").value(0));
		mockMvc.perform(get(URL)).andExpect(jsonPath("$.length()").value(3));
	}

	@Test
	void ac3_deletingATemplateLeavesDiscriminationOfIssuedRpsUntouched() throws Exception {
		String id = create("01.05", "Desenvolvimento de software sob demanda");
		CompanyId companyId = CompanyId.of(UUID.randomUUID());
		companyRepositoryPort.save(Company.of(companyId, Document.cnpj("11222333000181"), "123456789", "987654",
				"6201500", TaxRegime.LUCRO_PRESUMIDO, false, SefazEnvironment.HOMOLOGATION, "Rua Teste, 100", "SP",
				"nfse@example.com", "11999999999", null, null));
		NfseTomador tomador = NfseTomador.of(PersonRef.of(UUID.randomUUID()), "11222333000181", PersonType.COMPANY,
				"Tomador SA", "3304557", null);
		NfseDocument rps = nfseRepositoryPort.save(NfseDocument.issueRps(NfseId.of(UUID.randomUUID()), companyId,
				"3550308", tomador, ServiceCode.of("01.05"), PlaceOfProvision.PROVIDER, "3550308",
				new BigDecimal("1000.00"), new BigDecimal("5.0000"), new BigDecimal("50.00"), null, List.of(),
				"Desenvolvimento de software sob demanda", "RPS", 1L, Instant.parse("2026-10-01T10:00:00Z")));

		mockMvc.perform(delete(URL + "/" + id)).andExpect(status().isNoContent());

		assertThat(nfseRepositoryPort.findById(rps.getId()).orElseThrow().getDiscrimination())
				.isEqualTo("Desenvolvimento de software sob demanda");
	}

	@Test
	void unknownTemplateIsNotFoundOnUpdateAndDelete() throws Exception {
		String unknown = UUID.randomUUID().toString();

		mockMvc.perform(put(URL + "/" + unknown).contentType(MediaType.APPLICATION_JSON)
				.content("{ \"serviceCode\": \"01.05\", \"templateText\": \"x\" }")).andExpect(status().isNotFound());
		mockMvc.perform(delete(URL + "/" + unknown)).andExpect(status().isNotFound());
	}

	@Test
	void rejectsMissingBlankOrInvalidInput() throws Exception {
		mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{ \"serviceCode\": \"01.05\" }"))
				.andExpect(status().is4xxClientError());
		mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
				.content("{ \"serviceCode\": \"01.05\", \"templateText\": \"  \" }"))
				.andExpect(status().is4xxClientError());
		mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
				.content("{ \"serviceCode\": \"99.99\", \"templateText\": \"x\" }")).andExpect(status().isConflict());
		mockMvc.perform(get(URL).param("serviceCode", "abc")).andExpect(status().isConflict());
		assertThat(jpaRepository.count()).isZero();
	}
}
