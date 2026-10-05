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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PhysicalCountTest {

	@Test
	@DisplayName("Starting a TOTAL count does not require a product group")
	void startingATotalCountRequiresNoProductGroup() {
		final PhysicalCount count = PhysicalCount.start(PhysicalCountId.of(UUID.randomUUID()), PhysicalCountScope.TOTAL,
				null, UUID.randomUUID(), UUID.randomUUID(), Instant.now(),
				List.of(new PhysicalCountLine(UUID.randomUUID(), new BigDecimal("10"))));

		assertThat(count.getStatus()).isEqualTo(PhysicalCountStatus.IN_PROGRESS);
		assertThat(count.getProductGroupId()).isNull();
	}

	@Test
	@DisplayName("Starting a PARTIAL_BY_GROUP count requires a product group id")
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
	@DisplayName("A count starts IN_PROGRESS regardless of its scope")
	void countAlwaysStartsInProgressRegardlessOfScope() {
		final PhysicalCount count = PhysicalCount.start(PhysicalCountId.of(UUID.randomUUID()),
				PhysicalCountScope.PARTIAL_BY_GROUP, "Beverages", UUID.randomUUID(), UUID.randomUUID(), Instant.now(),
				List.of());

		assertThat(count.getStatus()).isEqualTo(PhysicalCountStatus.IN_PROGRESS);
		assertThat(count.getStatus()).isNotEqualTo(PhysicalCountStatus.PENDING_APPROVAL);
		assertThat(count.getStatus()).isNotEqualTo(PhysicalCountStatus.APPROVED);
	}

	@Test
	@DisplayName("Submitting counts for every line moves the count to PENDING_APPROVAL")
	void submittingCountsForEveryLineMovesTheCountToPendingApproval() {
		final UUID productA = UUID.randomUUID();
		final UUID productB = UUID.randomUUID();
		final PhysicalCount inProgress = PhysicalCount.of(PhysicalCountId.of(UUID.randomUUID()), PhysicalCountScope.TOTAL,
				null, UUID.randomUUID(), PhysicalCountStatus.IN_PROGRESS, UUID.randomUUID(), Instant.now(),
				List.of(new PhysicalCountLine(productA, new BigDecimal("10")),
						new PhysicalCountLine(productB, new BigDecimal("5"))));

		final PhysicalCount submitted = inProgress
				.submitCounts(Map.of(productA, new BigDecimal("8"), productB, new BigDecimal("5")));

		assertThat(submitted.getStatus()).isEqualTo(PhysicalCountStatus.PENDING_APPROVAL);
		assertThat(submitted.getLines()).extracting(PhysicalCountLine::countedQuantity)
				.containsExactlyInAnyOrder(new BigDecimal("8"), new BigDecimal("5"));
	}

	@Test
	@DisplayName("Submitting counts for only some lines keeps the count IN_PROGRESS and preserves earlier counts")
	void submittingCountsForOnlySomeLinesStaysInProgressAndKeepsWhatWasAlreadyCounted() {
		final UUID productA = UUID.randomUUID();
		final UUID productB = UUID.randomUUID();
		final PhysicalCount inProgress = PhysicalCount.of(PhysicalCountId.of(UUID.randomUUID()), PhysicalCountScope.TOTAL,
				null, UUID.randomUUID(), PhysicalCountStatus.IN_PROGRESS, UUID.randomUUID(), Instant.now(),
				List.of(new PhysicalCountLine(productA, new BigDecimal("10")),
						new PhysicalCountLine(productB, new BigDecimal("5"))));

		final PhysicalCount firstSubmission = inProgress.submitCounts(Map.of(productA, new BigDecimal("8")));
		assertThat(firstSubmission.getStatus()).isEqualTo(PhysicalCountStatus.IN_PROGRESS);

		final PhysicalCount secondSubmission = firstSubmission.submitCounts(Map.of(productB, new BigDecimal("5")));
		assertThat(secondSubmission.getStatus()).isEqualTo(PhysicalCountStatus.PENDING_APPROVAL);
		assertThat(secondSubmission.getLines()).extracting(PhysicalCountLine::productId, PhysicalCountLine::countedQuantity)
				.containsExactlyInAnyOrder(tuple(productA, new BigDecimal("8")), tuple(productB, new BigDecimal("5")));
	}

	@Test
	@DisplayName("Submitting counts for a count that is not IN_PROGRESS is rejected")
	void submittingCountsForACountThatIsNotInProgressIsRejected() {
		final UUID product = UUID.randomUUID();
		final PhysicalCount pending = PhysicalCount.of(PhysicalCountId.of(UUID.randomUUID()), PhysicalCountScope.TOTAL, null,
				UUID.randomUUID(), PhysicalCountStatus.PENDING_APPROVAL, UUID.randomUUID(), Instant.now(),
				List.of(new PhysicalCountLine(product, new BigDecimal("10"))));

		assertThatThrownBy(() -> pending.submitCounts(Map.of(product, new BigDecimal("10"))))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	@DisplayName("Submitting a count for a product that is not part of the count is rejected")
	void submittingACountForAProductNotInTheCountIsRejected() {
		final UUID knownProduct = UUID.randomUUID();
		final UUID unknownProduct = UUID.randomUUID();
		final PhysicalCount inProgress = PhysicalCount.of(PhysicalCountId.of(UUID.randomUUID()), PhysicalCountScope.TOTAL,
				null, UUID.randomUUID(), PhysicalCountStatus.IN_PROGRESS, UUID.randomUUID(), Instant.now(),
				List.of(new PhysicalCountLine(knownProduct, new BigDecimal("10"))));

		assertThatThrownBy(() -> inProgress.submitCounts(Map.of(unknownProduct, new BigDecimal("1"))))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	@DisplayName("Submitting a negative counted quantity is rejected")
	void submittingANegativeCountedQuantityIsRejected() {
		final UUID product = UUID.randomUUID();
		final PhysicalCount inProgress = PhysicalCount.of(PhysicalCountId.of(UUID.randomUUID()), PhysicalCountScope.TOTAL,
				null, UUID.randomUUID(), PhysicalCountStatus.IN_PROGRESS, UUID.randomUUID(), Instant.now(),
				List.of(new PhysicalCountLine(product, new BigDecimal("10"))));

		assertThatThrownBy(() -> inProgress.submitCounts(Map.of(product, new BigDecimal("-1"))))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	@DisplayName("Approving a PENDING_APPROVAL count moves it to APPROVED")
	void approvingAPendingApprovalCountMovesItToApproved() {
		final PhysicalCount pending = PhysicalCount.of(PhysicalCountId.of(UUID.randomUUID()), PhysicalCountScope.TOTAL, null,
				UUID.randomUUID(), PhysicalCountStatus.PENDING_APPROVAL, UUID.randomUUID(), Instant.now(), List.of());

		final PhysicalCount approved = pending.approve();

		assertThat(approved.getStatus()).isEqualTo(PhysicalCountStatus.APPROVED);
	}

	@Test
	@DisplayName("Approving a count that is not PENDING_APPROVAL is rejected")
	void approvingACountThatIsNotPendingApprovalIsRejected() {
		final PhysicalCount inProgress = PhysicalCount.of(PhysicalCountId.of(UUID.randomUUID()), PhysicalCountScope.TOTAL,
				null, UUID.randomUUID(), PhysicalCountStatus.IN_PROGRESS, UUID.randomUUID(), Instant.now(), List.of());
		final PhysicalCount alreadyApproved = PhysicalCount.of(PhysicalCountId.of(UUID.randomUUID()),
				PhysicalCountScope.TOTAL, null, UUID.randomUUID(), PhysicalCountStatus.APPROVED, UUID.randomUUID(),
				Instant.now(), List.of());

		assertThatThrownBy(inProgress::approve).isInstanceOf(BusinessRuleException.class);
		assertThatThrownBy(alreadyApproved::approve).isInstanceOf(BusinessRuleException.class);
	}

	@Test
	@DisplayName("Reconstructing a count from persistence still enforces the product-group invariant")
	void reconstructingFromPersistenceStillEnforcesTheGroupInvariant() {
		assertThatThrownBy(() -> PhysicalCount.of(PhysicalCountId.of(UUID.randomUUID()),
				PhysicalCountScope.PARTIAL_BY_GROUP, null, UUID.randomUUID(), PhysicalCountStatus.PENDING_APPROVAL,
				UUID.randomUUID(), Instant.now(), List.of()))
				.isInstanceOf(BusinessRuleException.class);
	}
}
