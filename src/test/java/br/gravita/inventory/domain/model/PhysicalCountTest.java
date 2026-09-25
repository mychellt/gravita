package br.gravita.inventory.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.inventory.PhysicalCount;
import br.gravita.core.domain.inventory.PhysicalCountId;
import br.gravita.core.domain.inventory.PhysicalCountLine;
import br.gravita.core.domain.inventory.PhysicalCountScope;
import br.gravita.core.domain.inventory.PhysicalCountStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PhysicalCountTest {

	@Test
	void startingATotalCountRequiresNoProductGroup() {
		PhysicalCount count = PhysicalCount.start(PhysicalCountId.of(UUID.randomUUID()), PhysicalCountScope.TOTAL,
				null, UUID.randomUUID(), UUID.randomUUID(), Instant.now(),
				List.of(new PhysicalCountLine(UUID.randomUUID(), new BigDecimal("10"))));

		assertThat(count.getStatus()).isEqualTo(PhysicalCountStatus.IN_PROGRESS);
		assertThat(count.getProductGroupId()).isNull();
	}

	@Test
	void startingAPartialByGroupCountRequiresAProductGroupId() {
		assertThatThrownBy(() -> PhysicalCount.start(PhysicalCountId.of(UUID.randomUUID()),
				PhysicalCountScope.PARTIAL_BY_GROUP, null, UUID.randomUUID(), UUID.randomUUID(), Instant.now(),
				List.of()))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("productGroupId");

		assertThatThrownBy(() -> PhysicalCount.start(PhysicalCountId.of(UUID.randomUUID()),
				PhysicalCountScope.PARTIAL_BY_GROUP, "  ", UUID.randomUUID(), UUID.randomUUID(), Instant.now(),
				List.of()))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void aCountAlwaysStartsInProgressRegardlessOfScope() {
		PhysicalCount count = PhysicalCount.start(PhysicalCountId.of(UUID.randomUUID()),
				PhysicalCountScope.PARTIAL_BY_GROUP, "Beverages", UUID.randomUUID(), UUID.randomUUID(), Instant.now(),
				List.of());

		assertThat(count.getStatus()).isEqualTo(PhysicalCountStatus.IN_PROGRESS);
		assertThat(count.getStatus()).isNotEqualTo(PhysicalCountStatus.PENDING_APPROVAL);
		assertThat(count.getStatus()).isNotEqualTo(PhysicalCountStatus.APPROVED);
	}

	@Test
	void submittingCountsForEveryLineMovesTheCountToPendingApproval() {
		UUID productA = UUID.randomUUID();
		UUID productB = UUID.randomUUID();
		PhysicalCount inProgress = PhysicalCount.of(PhysicalCountId.of(UUID.randomUUID()), PhysicalCountScope.TOTAL,
				null, UUID.randomUUID(), PhysicalCountStatus.IN_PROGRESS, UUID.randomUUID(), Instant.now(),
				List.of(new PhysicalCountLine(productA, new BigDecimal("10")),
						new PhysicalCountLine(productB, new BigDecimal("5"))));

		PhysicalCount submitted = inProgress
				.submitCounts(Map.of(productA, new BigDecimal("8"), productB, new BigDecimal("5")));

		assertThat(submitted.getStatus()).isEqualTo(PhysicalCountStatus.PENDING_APPROVAL);
		assertThat(submitted.getLines()).extracting(PhysicalCountLine::countedQuantity)
				.containsExactlyInAnyOrder(new BigDecimal("8"), new BigDecimal("5"));
	}

	@Test
	void submittingCountsForOnlySomeLinesStaysInProgressAndKeepsWhatWasAlreadyCounted() {
		UUID productA = UUID.randomUUID();
		UUID productB = UUID.randomUUID();
		PhysicalCount inProgress = PhysicalCount.of(PhysicalCountId.of(UUID.randomUUID()), PhysicalCountScope.TOTAL,
				null, UUID.randomUUID(), PhysicalCountStatus.IN_PROGRESS, UUID.randomUUID(), Instant.now(),
				List.of(new PhysicalCountLine(productA, new BigDecimal("10")),
						new PhysicalCountLine(productB, new BigDecimal("5"))));

		PhysicalCount firstSubmission = inProgress.submitCounts(Map.of(productA, new BigDecimal("8")));
		assertThat(firstSubmission.getStatus()).isEqualTo(PhysicalCountStatus.IN_PROGRESS);

		PhysicalCount secondSubmission = firstSubmission.submitCounts(Map.of(productB, new BigDecimal("5")));
		assertThat(secondSubmission.getStatus()).isEqualTo(PhysicalCountStatus.PENDING_APPROVAL);
		assertThat(secondSubmission.getLines()).extracting(PhysicalCountLine::productId, PhysicalCountLine::countedQuantity)
				.containsExactlyInAnyOrder(tuple(productA, new BigDecimal("8")), tuple(productB, new BigDecimal("5")));
	}

	@Test
	void submittingCountsForACountThatIsNotInProgressIsRejected() {
		UUID product = UUID.randomUUID();
		PhysicalCount pending = PhysicalCount.of(PhysicalCountId.of(UUID.randomUUID()), PhysicalCountScope.TOTAL, null,
				UUID.randomUUID(), PhysicalCountStatus.PENDING_APPROVAL, UUID.randomUUID(), Instant.now(),
				List.of(new PhysicalCountLine(product, new BigDecimal("10"))));

		assertThatThrownBy(() -> pending.submitCounts(Map.of(product, new BigDecimal("10"))))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void submittingACountForAProductNotInTheCountIsRejected() {
		UUID knownProduct = UUID.randomUUID();
		UUID unknownProduct = UUID.randomUUID();
		PhysicalCount inProgress = PhysicalCount.of(PhysicalCountId.of(UUID.randomUUID()), PhysicalCountScope.TOTAL,
				null, UUID.randomUUID(), PhysicalCountStatus.IN_PROGRESS, UUID.randomUUID(), Instant.now(),
				List.of(new PhysicalCountLine(knownProduct, new BigDecimal("10"))));

		assertThatThrownBy(() -> inProgress.submitCounts(Map.of(unknownProduct, new BigDecimal("1"))))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void submittingANegativeCountedQuantityIsRejected() {
		UUID product = UUID.randomUUID();
		PhysicalCount inProgress = PhysicalCount.of(PhysicalCountId.of(UUID.randomUUID()), PhysicalCountScope.TOTAL,
				null, UUID.randomUUID(), PhysicalCountStatus.IN_PROGRESS, UUID.randomUUID(), Instant.now(),
				List.of(new PhysicalCountLine(product, new BigDecimal("10"))));

		assertThatThrownBy(() -> inProgress.submitCounts(Map.of(product, new BigDecimal("-1"))))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void approvingAPendingApprovalCountMovesItToApproved() {
		PhysicalCount pending = PhysicalCount.of(PhysicalCountId.of(UUID.randomUUID()), PhysicalCountScope.TOTAL, null,
				UUID.randomUUID(), PhysicalCountStatus.PENDING_APPROVAL, UUID.randomUUID(), Instant.now(), List.of());

		PhysicalCount approved = pending.approve();

		assertThat(approved.getStatus()).isEqualTo(PhysicalCountStatus.APPROVED);
	}

	@Test
	void approvingACountThatIsNotPendingApprovalIsRejected() {
		PhysicalCount inProgress = PhysicalCount.of(PhysicalCountId.of(UUID.randomUUID()), PhysicalCountScope.TOTAL,
				null, UUID.randomUUID(), PhysicalCountStatus.IN_PROGRESS, UUID.randomUUID(), Instant.now(), List.of());
		PhysicalCount alreadyApproved = PhysicalCount.of(PhysicalCountId.of(UUID.randomUUID()),
				PhysicalCountScope.TOTAL, null, UUID.randomUUID(), PhysicalCountStatus.APPROVED, UUID.randomUUID(),
				Instant.now(), List.of());

		assertThatThrownBy(inProgress::approve).isInstanceOf(BusinessRuleException.class);
		assertThatThrownBy(alreadyApproved::approve).isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void reconstructingFromPersistenceStillEnforcesTheGroupInvariant() {
		assertThatThrownBy(() -> PhysicalCount.of(PhysicalCountId.of(UUID.randomUUID()),
				PhysicalCountScope.PARTIAL_BY_GROUP, null, UUID.randomUUID(), PhysicalCountStatus.PENDING_APPROVAL,
				UUID.randomUUID(), Instant.now(), List.of()))
				.isInstanceOf(BusinessRuleException.class);
	}
}
