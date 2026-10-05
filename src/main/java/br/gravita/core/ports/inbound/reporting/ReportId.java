package br.gravita.core.ports.inbound.reporting;

import java.util.Arrays;
import java.util.Optional;

/**
 * The reports of the module that {@link ExportReportUseCase} can export. {@code slug} is the report's path segment
 * in {@code /api/reports/{slug}} and the name of its screen in the permission profiles.
 */
public enum ReportId {
	DASHBOARD("dashboard"),
	ABC_CURVE("abc-curve"),
	DRE("dre"),
	STOCK_TURNOVER("stock-turnover"),
	COMMISSIONS("commissions"),
	PURCHASES_BY_SUPPLIER("purchases-by-supplier"),
	FISCAL_BOOKS("fiscal-books"),
	ASSESSED_TAXES("assessed-taxes");

	private final String slug;

	ReportId(final String slug) {
		this.slug = slug;
	}

	public String slug() {
		return slug;
	}

	public static Optional<ReportId> fromSlug(final String slug) {
		return Arrays.stream(values()).filter(id -> id.slug.equalsIgnoreCase(slug == null ? "" : slug.trim()))
				.findFirst();
	}
}
