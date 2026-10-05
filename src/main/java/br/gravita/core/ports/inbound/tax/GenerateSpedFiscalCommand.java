package br.gravita.core.ports.inbound.tax;

import br.gravita.core.domain.masterdata.CompanyId;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Objects;

/**
 * {@code taxpayer} and {@code accountant} carry what the EFD's identification records (0000, 0005, 0100) require
 * and {@code tax} does not hold - the company's legal name and IBGE municipality, its activity profile, the
 * accountant's registration - so whoever requests the file states them.
 */
public record GenerateSpedFiscalCommand(CompanyId companyId, Period period, Finality finality, Taxpayer taxpayer,
		Accountant accountant) {

	public GenerateSpedFiscalCommand {
		Objects.requireNonNull(companyId, "companyId is required");
		Objects.requireNonNull(period, "period is required");
		finality = finality == null ? Finality.ORIGINAL : finality;
		Objects.requireNonNull(taxpayer, "taxpayer is required");
		Objects.requireNonNull(accountant, "accountant is required");
	}

	/** The days the file covers, both ends included. An EFD covers one calendar month, whole or part of it. */
	public record Period(LocalDate start, LocalDate end) {

		public Period {
			Objects.requireNonNull(start, "period start is required");
			Objects.requireNonNull(end, "period end is required");
			if (end.isBefore(start)) {
				throw new IllegalArgumentException("period end must not be before its start");
			}
		}

		public static Period ofMonth(final YearMonth month) {
			return new Period(month.atDay(1), month.atEndOfMonth());
		}

		public boolean isWithinOneMonth() {
			return YearMonth.from(start).equals(YearMonth.from(end));
		}
	}

	/** Whether the file is the first one for the period or replaces one already sent ({@code COD_FIN}). */
	public enum Finality {
		ORIGINAL("0"), SUBSTITUTE("1");

		private final String code;

		Finality(final String code) {
			this.code = code;
		}

		public String code() {
			return code;
		}
	}

	/** {@code IND_PERFIL}: the profile the taxpayer's EFD is filed under. */
	public enum ActivityProfile {
		A, B, C
	}

	/** {@code IND_ATIV}: industrial or equivalent (0), or any other activity (1). */
	public enum ActivityType {
		INDUSTRIAL("0"), OTHER("1");

		private final String code;

		ActivityType(final String code) {
			this.code = code;
		}

		public String code() {
			return code;
		}
	}

	/** The taxpayer's registration data as the state knows it; {@code municipalityCode} is the 7-digit IBGE code. */
	public record Taxpayer(String legalName, String municipalityCode, ActivityProfile profile, ActivityType activity,
			String tradeName, String zipCode, String number, String neighborhood) {
	}

	public record Accountant(String name, String cpf, String crc, String email) {
	}
}
