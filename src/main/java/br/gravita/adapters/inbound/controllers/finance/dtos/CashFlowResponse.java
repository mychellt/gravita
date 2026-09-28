package br.gravita.adapters.inbound.controllers.finance.dtos;

import br.gravita.core.domain.finance.CashFlowBucket;
import br.gravita.core.domain.finance.CashFlowGranularity;
import br.gravita.core.domain.finance.CashFlowProjection;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CashFlowResponse(CashFlowGranularity granularity, LocalDate from, LocalDate to,
		BigDecimal openingBalance, BigDecimal closingBalance, List<BucketResponse> buckets) {

	public record BucketResponse(LocalDate periodStart, LocalDate periodEnd, BigDecimal realizedInflow,
			BigDecimal realizedOutflow, BigDecimal projectedInflow, BigDecimal projectedOutflow, BigDecimal net,
			BigDecimal balance) {

		static BucketResponse from(CashFlowBucket bucket) {
			return new BucketResponse(bucket.periodStart(), bucket.periodEnd(), bucket.realizedInflow(),
					bucket.realizedOutflow(), bucket.projectedInflow(), bucket.projectedOutflow(), bucket.net(),
					bucket.balance());
		}
	}

	public static CashFlowResponse from(CashFlowProjection projection) {
		return new CashFlowResponse(projection.getGranularity(), projection.getFrom(), projection.getTo(),
				projection.getOpeningBalance(), projection.getClosingBalance(),
				projection.getBuckets().stream().map(BucketResponse::from).toList());
	}
}
