package br.gravita.core.domain.sales;

import br.gravita.core.domain.shared.BusinessRuleException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

@Getter
public final class Opportunity {

	private final OpportunityId id;
	private final UUID customerId;
	private final BigDecimal estimatedValue;
	private final Integer probability;
	private final LocalDate expectedCloseDate;
	private final UUID owner;
	private final OpportunityStage stage;

	private Opportunity(OpportunityId id, UUID customerId, BigDecimal estimatedValue, Integer probability,
			LocalDate expectedCloseDate, UUID owner, OpportunityStage stage) {
		this.id = Objects.requireNonNull(id, "id is required");
		this.customerId = Objects.requireNonNull(customerId, "customerId is required");
		this.estimatedValue = requireNonNegative(estimatedValue);
		this.probability = requireValidProbability(probability);
		this.expectedCloseDate = Objects.requireNonNull(expectedCloseDate, "expectedCloseDate is required");
		this.owner = Objects.requireNonNull(owner, "owner is required");
		this.stage = Objects.requireNonNull(stage, "stage is required");
	}

	public static Opportunity create(OpportunityId id, UUID customerId, BigDecimal estimatedValue,
			Integer probability, LocalDate expectedCloseDate, UUID owner) {
		return new Opportunity(id, customerId, estimatedValue, probability, expectedCloseDate, owner,
				OpportunityStage.PROSPECTING);
	}

	public static Opportunity of(OpportunityId id, UUID customerId, BigDecimal estimatedValue, Integer probability,
			LocalDate expectedCloseDate, UUID owner, OpportunityStage stage) {
		return new Opportunity(id, customerId, estimatedValue, probability, expectedCloseDate, owner, stage);
	}

	public Opportunity withUpdatedFields(UUID customerId, BigDecimal estimatedValue, Integer probability,
			LocalDate expectedCloseDate, UUID owner) {
		assertNotTerminal("update");
		return new Opportunity(id,
				coalesce(customerId, this.customerId),
				coalesce(estimatedValue, this.estimatedValue),
				coalesce(probability, this.probability),
				coalesce(expectedCloseDate, this.expectedCloseDate),
				coalesce(owner, this.owner),
				stage);
	}

	public Opportunity moveToStage(OpportunityStage newStage) {
		assertNotTerminal("change the stage of");
		Objects.requireNonNull(newStage, "newStage is required");
		if (newStage == stage) {
			throw new BusinessRuleException("Opportunity is already in stage " + stage);
		}
		return new Opportunity(id, customerId, estimatedValue, probability, expectedCloseDate, owner, newStage);
	}

	private void assertNotTerminal(String action) {
		if (stage.isTerminal()) {
			throw new BusinessRuleException("Cannot " + action + " an opportunity in a terminal stage: " + stage);
		}
	}

	private static <T> T coalesce(T newValue, T currentValue) {
		return newValue != null ? newValue : currentValue;
	}

	private static BigDecimal requireNonNegative(BigDecimal estimatedValue) {
		if (estimatedValue == null) {
			throw new BusinessRuleException("estimatedValue is required");
		}
		if (estimatedValue.compareTo(BigDecimal.ZERO) < 0) {
			throw new BusinessRuleException("estimatedValue cannot be negative: " + estimatedValue);
		}
		return estimatedValue;
	}

	private static Integer requireValidProbability(Integer probability) {
		if (probability == null) {
			throw new BusinessRuleException("probability is required");
		}
		if (probability < 0 || probability > 100) {
			throw new BusinessRuleException("probability must be between 0 and 100: " + probability);
		}
		return probability;
	}
}
