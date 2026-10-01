package br.gravita.tax.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gravita.adapters.outbound.persistence.entities.tax.MunicipalityIntegrationJpaEntity;
import br.gravita.adapters.outbound.persistence.repositories.tax.MunicipalityIntegrationJpaRepository;
import br.gravita.core.domain.tax.NfseStandard;
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
class RegisterMunicipalityIntegrationEndToEndTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private MunicipalityIntegrationJpaRepository jpaRepository;

	private String register(String ibgeCode, String body) throws Exception {
		return mockMvc.perform(post("/api/municipalities/" + ibgeCode + "/integration")
						.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").exists())
				.andReturn().getResponse().getContentAsString();
	}

	@Test
	void ac1and2_registersANonHomologatedMunicipalityWithoutAnAdapterForItsStandard() throws Exception {
		register("3550308", """
				{ "standard": "BETHA", "requiredCertificateType": "A1", "requiredFields": ["inscricaoMunicipal"],
				  "homologated": false }
				""");

		MunicipalityIntegrationJpaEntity persisted = jpaRepository.findByIbgeCode("3550308").orElseThrow();
		assertThat(persisted.getStandard()).isEqualTo(NfseStandard.BETHA);
		assertThat(persisted.isHomologated()).isFalse();
		assertThat(persisted.getRequiredFields()).containsExactly("inscricaoMunicipal");
	}

	@Test
	void ac3_reRegisteringTheSameIbgeCodeUpdatesInPlace() throws Exception {
		String first = register("4106902", """
				{ "standard": "ABRASF", "version": "2.03", "webserviceUrl": "https://old.example/ws",
				  "requiredCertificateType": "A1", "homologated": false }
				""");
		String second = register("4106902", """
				{ "standard": "ISSNET", "version": "1.0", "webserviceUrl": "https://new.example/ws",
				  "requiredCertificateType": "A3", "requiredFields": ["x", "y"], "homologated": true }
				""");

		assertThat(second).isEqualTo(first);
		assertThat(jpaRepository.findAll()).hasSize(1);
		MunicipalityIntegrationJpaEntity persisted = jpaRepository.findByIbgeCode("4106902").orElseThrow();
		assertThat(persisted.getStandard()).isEqualTo(NfseStandard.ISSNET);
		assertThat(persisted.getWebserviceUrl()).isEqualTo("https://new.example/ws");
		assertThat(persisted.isHomologated()).isTrue();
		assertThat(persisted.getRequiredFields()).containsExactly("x", "y");
	}

	@Test
	void rejectsUnknownStandardAndMalformedIbgeCode() throws Exception {
		mockMvc.perform(post("/api/municipalities/3550308/integration").contentType(MediaType.APPLICATION_JSON)
				.content("{ \"standard\": \"WEBISS\", \"requiredCertificateType\": \"A1\" }"))
				.andExpect(status().is4xxClientError());
		mockMvc.perform(post("/api/municipalities/12/integration").contentType(MediaType.APPLICATION_JSON)
				.content("{ \"standard\": \"ABRASF\", \"requiredCertificateType\": \"A1\" }"))
				.andExpect(status().isConflict());
	}

	@Test
	void homologatedDefaultsToFalseWhenOmitted() throws Exception {
		register("3550308", "{ \"standard\": \"ABRASF\", \"requiredCertificateType\": \"A1\" }");

		assertThat(jpaRepository.findByIbgeCode("3550308").orElseThrow().isHomologated()).isFalse();
	}
}
