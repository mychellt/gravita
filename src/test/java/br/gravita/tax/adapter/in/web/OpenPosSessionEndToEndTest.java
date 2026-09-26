package br.gravita.tax.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gravita.adapters.outbound.persistence.entities.tax.PosSessionJpaEntity;
import br.gravita.adapters.outbound.persistence.repositories.tax.PosSessionJpaRepository;
import br.gravita.core.domain.tax.PosSessionStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * GRA-89: end-to-end verification of OpenPosSessionUseCase
 * (POST /api/pdv/sessions) through the real HTTP stack.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class OpenPosSessionEndToEndTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PosSessionJpaRepository posSessionJpaRepository;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void ac2and3_openingASessionCreatesItOpenAndLinkedToTheOperatorAndRegister() throws Exception {
		UUID registerId = UUID.randomUUID();
		UUID operatorId = UUID.randomUUID();

		String response = mockMvc.perform(post("/api/pdv/sessions")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "registerId": "%s", "operatorId": "%s", "openingChangeAmount": 100.00 }
								""".formatted(registerId, operatorId)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").exists())
				.andReturn().getResponse().getContentAsString();

		UUID sessionId = UUID.fromString(objectMapper.readTree(response).get("id").asString());
		PosSessionJpaEntity persisted = posSessionJpaRepository.findById(sessionId).orElseThrow();
		assertThat(persisted.getStatus()).isEqualTo(PosSessionStatus.OPEN);
		assertThat(persisted.getRegisterId()).isEqualTo(registerId);
		assertThat(persisted.getOperatorId()).isEqualTo(operatorId);
		assertThat(persisted.getOpeningChangeAmount()).isEqualByComparingTo("100.00");
		assertThat(persisted.getOpenedAt()).isNotNull();
	}

	@Test
	void ac1_openingASecondSessionOnAnAlreadyOpenRegisterIsRejected() throws Exception {
		UUID registerId = UUID.randomUUID();
		seedOpenSession(registerId);

		mockMvc.perform(post("/api/pdv/sessions")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "registerId": "%s", "operatorId": "%s", "openingChangeAmount": 50.00 }
								""".formatted(registerId, UUID.randomUUID())))
				.andExpect(status().isConflict());
	}

	@Test
	void ac4_multipleRegistersCanEachHoldAnIndependentOpenSessionConcurrently() throws Exception {
		UUID registerA = UUID.randomUUID();
		UUID registerB = UUID.randomUUID();

		mockMvc.perform(post("/api/pdv/sessions")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "registerId": "%s", "operatorId": "%s", "openingChangeAmount": 50.00 }
								""".formatted(registerA, UUID.randomUUID())))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/pdv/sessions")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "registerId": "%s", "operatorId": "%s", "openingChangeAmount": 75.00 }
								""".formatted(registerB, UUID.randomUUID())))
				.andExpect(status().isCreated());

		assertThat(posSessionJpaRepository.existsByRegisterIdAndStatus(registerA, PosSessionStatus.OPEN)).isTrue();
		assertThat(posSessionJpaRepository.existsByRegisterIdAndStatus(registerB, PosSessionStatus.OPEN)).isTrue();
	}

	private void seedOpenSession(UUID registerId) {
		posSessionJpaRepository.save(PosSessionJpaEntity.builder()
				.id(UUID.randomUUID())
				.registerId(registerId)
				.operatorId(UUID.randomUUID())
				.openingChangeAmount(new BigDecimal("50.00"))
				.status(PosSessionStatus.OPEN)
				.openedAt(Instant.now())
				.build());
	}
}
