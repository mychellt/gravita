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
class SubmitPhysicalCountEndToEndTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PhysicalCountJpaRepository physicalCountJpaRepository;

	@Test
	void submittingCountsForEveryLineMovesTheCountToPendingApprovalAndUnblocksApprove() throws Exception {
		UUID warehouseId = UUID.randomUUID();
		UUID divergentProduct = UUID.randomUUID();
		UUID matchingProduct = UUID.randomUUID();
		UUID countId = seedInProgressCount(warehouseId,
				List.of(line(divergentProduct, "10"), line(matchingProduct, "20")));

		mockMvc.perform(post("/api/inventory/counts/" + countId + "/submit")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "countedLines": [
								    { "productId": "%s", "countedQuantity": 7 },
								    { "productId": "%s", "countedQuantity": 20 }
								  ],
								  "submittedBy": "%s"
								}
								""".formatted(divergentProduct, matchingProduct, UUID.randomUUID())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("PENDING_APPROVAL"));

		PhysicalCountJpaEntity persisted = physicalCountJpaRepository.findById(countId).orElseThrow();
		assertThat(persisted.getStatus()).isEqualTo(PhysicalCountStatus.PENDING_APPROVAL);

		mockMvc.perform(post("/api/inventory/counts/" + countId + "/approve")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "approvedBy": "%s" }
								""".formatted(UUID.randomUUID())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("APPROVED"));
	}

	@Test
	void submittingCountsForOnlySomeLinesLeavesTheCountInProgress() throws Exception {
		UUID warehouseId = UUID.randomUUID();
		UUID productA = UUID.randomUUID();
		UUID productB = UUID.randomUUID();
		UUID countId = seedInProgressCount(warehouseId, List.of(line(productA, "10"), line(productB, "5")));

		mockMvc.perform(post("/api/inventory/counts/" + countId + "/submit")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "countedLines": [ { "productId": "%s", "countedQuantity": 8 } ],
								  "submittedBy": "%s"
								}
								""".formatted(productA, UUID.randomUUID())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("IN_PROGRESS"));

		PhysicalCountJpaEntity persisted = physicalCountJpaRepository.findById(countId).orElseThrow();
		assertThat(persisted.getStatus()).isEqualTo(PhysicalCountStatus.IN_PROGRESS);
	}

	@Test
	void submittingCountsForACountThatIsNotInProgressIsRejected() throws Exception {
		UUID warehouseId = UUID.randomUUID();
		UUID product = UUID.randomUUID();
		UUID countId = seedCount(warehouseId, PhysicalCountStatus.PENDING_APPROVAL, List.of(line(product, "10")));

		mockMvc.perform(post("/api/inventory/counts/" + countId + "/submit")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "countedLines": [ { "productId": "%s", "countedQuantity": 10 } ],
								  "submittedBy": "%s"
								}
								""".formatted(product, UUID.randomUUID())))
				.andExpect(status().isConflict());

		PhysicalCountJpaEntity persisted = physicalCountJpaRepository.findById(countId).orElseThrow();
		assertThat(persisted.getStatus()).isEqualTo(PhysicalCountStatus.PENDING_APPROVAL);
	}

	private PhysicalCountLineEmbeddable line(UUID productId, String systemQuantity) {
		return PhysicalCountLineEmbeddable.builder()
				.productId(productId)
				.systemQuantity(new BigDecimal(systemQuantity))
				.build();
	}

	private UUID seedInProgressCount(UUID warehouseId, List<PhysicalCountLineEmbeddable> lines) {
		return seedCount(warehouseId, PhysicalCountStatus.IN_PROGRESS, lines);
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
