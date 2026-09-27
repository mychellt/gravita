package br.gravita.tax.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gravita.adapters.outbound.persistence.entities.tax.CashMovementJpaEntity;
import br.gravita.adapters.outbound.persistence.entities.tax.PosSessionJpaEntity;
import br.gravita.adapters.outbound.persistence.repositories.tax.CashMovementJpaRepository;
import br.gravita.adapters.outbound.persistence.repositories.tax.PosSessionJpaRepository;
import br.gravita.core.domain.masterdata.Company;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.SefazEnvironment;
import br.gravita.core.domain.masterdata.TaxRegime;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.domain.tax.CashMovementType;
import br.gravita.core.domain.tax.PosSessionStatus;
import br.gravita.core.ports.outbound.persistence.CompanyRepositoryPort;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class RecordCashMovementEndToEndTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PosSessionJpaRepository posSessionJpaRepository;

	@Autowired
	private CashMovementJpaRepository cashMovementJpaRepository;

	@Autowired
	private CompanyRepositoryPort companyRepositoryPort;

	@Autowired
	private ObjectMapper objectMapper;

	private CompanyId companyId;

	@BeforeEach
	void seedCompany() {
		companyId = CompanyId.of(UUID.randomUUID());
		companyRepositoryPort.save(Company.of(companyId, Document.cnpj("11222333000181"), "123456789", "987654",
				"6201500", TaxRegime.SIMPLES_NACIONAL, true, SefazEnvironment.HOMOLOGATION, "Rua Teste, 100", "SP",
				"nfce@example.com", "11999999999", null, null));
	}

	@Test
	void ac1and3and4_recordingASangriaAgainstAnOpenSessionPersistsItImmediatelyWithATimestampAndLink()
			throws Exception {
		UUID sessionId = seedOpenSession();

		String response = mockMvc.perform(post("/api/pdv/cash-movements")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "sessionId": "%s", "type": "SANGRIA", "amount": 50.00, "justification": "Bank deposit" }
								""".formatted(sessionId)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").exists())
				.andReturn().getResponse().getContentAsString();

		UUID movementId = UUID.fromString(objectMapper.readTree(response).get("id").asString());
		CashMovementJpaEntity persisted = cashMovementJpaRepository.findById(movementId).orElseThrow();
		assertThat(persisted.getType()).isEqualTo(CashMovementType.SANGRIA);
		assertThat(persisted.getSessionId()).isEqualTo(sessionId);
		assertThat(persisted.getAmount()).isEqualByComparingTo("50.00");
		assertThat(persisted.getJustification()).isEqualTo("Bank deposit");
		assertThat(persisted.getTimestamp()).isNotNull();
	}

	@Test
	void ac1_recordingASuprimentoIsAccepted() throws Exception {
		UUID sessionId = seedOpenSession();

		mockMvc.perform(post("/api/pdv/cash-movements")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "sessionId": "%s", "type": "SUPRIMENTO", "amount": 30.00, "justification": "Change top-up" }
								""".formatted(sessionId)))
				.andExpect(status().isCreated());
	}

	@Test
	void ac2_blankJustificationIsRejected() throws Exception {
		UUID sessionId = seedOpenSession();

		mockMvc.perform(post("/api/pdv/cash-movements")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "sessionId": "%s", "type": "SANGRIA", "amount": 50.00, "justification": "" }
								""".formatted(sessionId)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void ac4_recordingAgainstAClosedSessionIsRejected() throws Exception {
		UUID sessionId = seedClosedSession();

		mockMvc.perform(post("/api/pdv/cash-movements")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "sessionId": "%s", "type": "SANGRIA", "amount": 50.00, "justification": "Bank deposit" }
								""".formatted(sessionId)))
				.andExpect(status().isConflict());
	}

	@Test
	void ac4_recordingAgainstANonExistentSessionIsRejected() throws Exception {
		mockMvc.perform(post("/api/pdv/cash-movements")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "sessionId": "%s", "type": "SANGRIA", "amount": 50.00, "justification": "Bank deposit" }
								""".formatted(UUID.randomUUID())))
				.andExpect(status().isNotFound());
	}

	private UUID seedOpenSession() {
		UUID sessionId = UUID.randomUUID();
		posSessionJpaRepository.save(PosSessionJpaEntity.builder()
				.id(sessionId)
				.registerId(UUID.randomUUID())
				.operatorId(UUID.randomUUID())
				.companyId(companyId.value())
				.openingChangeAmount(new BigDecimal("100.00"))
				.status(PosSessionStatus.OPEN)
				.openedAt(Instant.now())
				.build());
		return sessionId;
	}

	private UUID seedClosedSession() {
		UUID sessionId = UUID.randomUUID();
		posSessionJpaRepository.save(PosSessionJpaEntity.builder()
				.id(sessionId)
				.registerId(UUID.randomUUID())
				.operatorId(UUID.randomUUID())
				.companyId(companyId.value())
				.openingChangeAmount(new BigDecimal("100.00"))
				.status(PosSessionStatus.CLOSED)
				.openedAt(Instant.now())
				.closedAt(Instant.now())
				.build());
		return sessionId;
	}
}
