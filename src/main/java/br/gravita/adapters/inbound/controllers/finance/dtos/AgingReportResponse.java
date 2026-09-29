package br.gravita.adapters.inbound.controllers.finance.dtos;

import br.gravita.core.domain.finance.AgingBucket;
import br.gravita.core.domain.finance.AgingRange;
import br.gravita.core.domain.finance.AgingReport;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AgingReportResponse(LocalDate asOfDate, BigDecimal total, int titleCount,
		List<BucketResponse> buckets) {

	/** {@code toDays} is {@code null} for the open-ended last range. */
	public record BucketResponse(AgingRange range, long fromDays, Long toDays, int titleCount, BigDecimal total) {

		static BucketResponse from(AgingBucket bucket) {
			return new BucketResponse(bucket.range(), bucket.range().fromDays(),
					bucket.range().toDays().orElse(null), bucket.titleCount(), bucket.total());
		}
	}

	public static AgingReportResponse from(AgingReport report) {
		return new AgingReportResponse(report.getAsOfDate(), report.getTotal(), report.getTitleCount(),
				report.getBuckets().stream().map(BucketResponse::from).toList());
	}
}
