package br.gravita.core.ports.outbound.tax;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** How SPED writes values: dates as {@code ddMMyyyy}, numbers with a comma and no thousands separator. */
public final class SpedValues {

	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("ddMMyyyy");

	private SpedValues() {
	}

	public static String date(LocalDate date) {
		return date == null ? null : DATE.format(date);
	}

	/** An amount of money: two decimal places. */
	public static String money(BigDecimal amount) {
		return decimal(amount, 2);
	}

	public static String decimal(BigDecimal value, int scale) {
		return value == null ? null : value.setScale(scale, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
	}
}
