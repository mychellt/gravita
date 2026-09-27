package br.gravita.inventory.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gravita.adapters.outbound.persistence.entities.inventory.PhysicalCountJpaEntity;
import br.gravita.adapters.outbound.persistence.entities.inventory.PhysicalCountLineEmbeddable;
import br.gravita.adapters.outbound.persistence.repositories.inventory.PhysicalCountJpaRepository;
import br.gravita.core.domain.inventory.PhysicalCountScope;
import br.gravita.core.domain.inventory.PhysicalCountStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
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
class ApprovePhysicalCountEndToEndTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PhysicalCountJpaRepository physicalCountJpaRepository;

	@Test
	void approvingADivergentPendingCountAdjustsBalancesAndMarksItApproved() throws Exception {
		UUID warehouseId = UUID.randomUUID();
		UUID divergentProduct = UUID.randomUUID();
		UUID matchingProduct = UUID.randomUUID();
		UUID countId = seedPendingCount(warehouseId,
				List.of(line(divergentProduct, "10", "7"), line(matchingProduct, "20", "20")));

		mockMvc.perform(post("/api/inventory/counts/" + countId + "/approve")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "approvedBy": "%s" }
								""".formatted(UUID.randomUUID())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("APPROVED"));

		PhysicalCountJpaEntity persisted = physicalCountJpaRepository.findById(countId).orElseThrow();
		assertThat(persisted.getStatus()).isEqualTo(PhysicalCountStatus.APPROVED);
	}

	@Test
	void approvingACountThatIsStillInProgressIsRejected() throws Exception {
		UUID warehouseId = UUID.randomUUID();
		UUID countId = seedCount(warehouseId, PhysicalCountStatus.IN_PROGRESS, List.of());

		mockMvc.perform(post("/api/inventory/counts/" + countId + "/approve")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "approvedBy": "%s" }
								""".formatted(UUID.randomUUID())))
				.andExpect(status().isConflict());

		PhysicalCountJpaEntity persisted = physicalCountJpaRepository.findById(countId).orElseThrow();
		assertThat(persisted.getStatus()).isEqualTo(PhysicalCountStatus.IN_PROGRESS);
	}

	private PhysicalCountLineEmbeddable line(UUID productId, String systemQuantity, String countedQuantity) {
		return PhysicalCountLineEmbeddable.builder()
				.productId(productId)
				.systemQuantity(new BigDecimal(systemQuantity))
				.countedQuantity(new BigDecimal(countedQuantity))
				.build();
	}

	private UUID seedPendingCount(UUID warehouseId, List<PhysicalCountLineEmbeddable> lines) {
		return seedCount(warehouseId, PhysicalCountStatus.PENDING_APPROVAL, lines);
	}

	private UUID seedCount(UUID warehouseId, PhysicalCountStatus status, List<PhysicalCountLineEmbeddable> lines) {
		UUID id = UUID.randomUUID();
		physicalCountJpaRepository.save(PhysicalCountJpaEntity.builder()
				.id(id)
				.scope(PhysicalCountScope.TOTAL)
				.warehouseId(warehouseId)
				.status(status)
				.startedBy(UUID.randomUUID())
				.startedAt(Instant.now())
				.lines(new ArrayList<>(lines))
				.build());
		return id;
	}
}
