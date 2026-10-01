package br.gravita.reporting.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.exceptions.ForbiddenException;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.ports.inbound.reporting.AssessedTaxSummary;
import br.gravita.core.ports.inbound.reporting.AssessedTaxesQuery;
import br.gravita.core.ports.outbound.reporting.PermissionCheckPort;
import br.gravita.core.ports.outbound.reporting.TaxReadModelPort;
import br.gravita.core.ports.outbound.reporting.TaxReadModelPort.DocumentTaxRecord;
import br.gravita.core.usercases.reporting.GetAssessedTaxesService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GetAssessedTaxesServiceTest {

	private static final YearMonth PERIOD = YearMonth.of(2028, 2);

	private final TaxReadModelPort tax = mock(TaxReadModelPort.class);
	private final PermissionCheckPort permissions = mock(PermissionCheckPort.class);

	private final UserId user = UserId.generate();
	private GetAssessedTaxesService service;

	@BeforeEach
	void setUp() {
		service = new GetAssessedTaxesService(tax, permissions);
		when(permissions.canView(user, "assessed-taxes")).thenReturn(true);
		when(tax.authorizedDocumentTaxes(any(), any())).thenReturn(List.of());
	}

	@DisplayName("Refuses a user whose profile cannot view the summary, without reading any data")
	@Test
	void refusesAUserWhoseProfileCannotViewTheSummaryWithoutReadingAnything() {
		UserId stranger = UserId.generate();
		when(permissions.canView(stranger, "assessed-taxes")).thenReturn(false);

		assertThatThrownBy(() -> service.execute(new AssessedTaxesQuery(stranger, PERIOD)))
				.isInstanceOf(ForbiddenException.class);

		verifyNoInteractions(tax);
	}

	@DisplayName("Reads the whole requested month from the tax module")
	@Test
	void readsTheWholeRequestedMonthFromTax() {
		service.execute(new AssessedTaxesQuery(user, PERIOD));

		verify(tax).authorizedDocumentTaxes(LocalDate.of(2028, 2, 1), LocalDate.of(2028, 2, 29));
	}

	@DisplayName("Sums each tax over the authorized NF-e and NFS-e")
	@Test
	void sumsEachTaxOverTheAuthorizedNfeAndNfse() {
		when(tax.authorizedDocumentTaxes(any(), any())).thenReturn(List.of(
				nfe("100.00", "10.00", "1.65", "7.60"),
				nfe("50.50", "5.25", "0.83", "3.80"),
				nfse("30.00")));

		AssessedTaxSummary summary = service.execute(new AssessedTaxesQuery(user, PERIOD));

		assertThat(summary.period()).isEqualTo(PERIOD);
		assertThat(summary.icms()).isEqualByComparingTo("150.50");
		assertThat(summary.ipi()).isEqualByComparingTo("15.25");
		assertThat(summary.pis()).isEqualByComparingTo("2.48");
		assertThat(summary.cofins()).isEqualByComparingTo("11.40");
		assertThat(summary.iss()).isEqualByComparingTo("30.00");
	}

	@DisplayName("Totals zero for every tax when the period has no authorized document")
	@Test
	void totalsZeroForEveryTaxWhenThePeriodHasNoAuthorizedDocument() {
		AssessedTaxSummary summary = service.execute(new AssessedTaxesQuery(user, PERIOD));

		assertThat(summary.icms()).isEqualByComparingTo("0");
		assertThat(summary.ipi()).isEqualByComparingTo("0");
		assertThat(summary.pis()).isEqualByComparingTo("0");
		assertThat(summary.cofins()).isEqualByComparingTo("0");
		assertThat(summary.iss()).isEqualByComparingTo("0");
	}

	private static DocumentTaxRecord nfe(String icms, String ipi, String pis, String cofins) {
		return new DocumentTaxRecord("NFE", LocalDate.of(2028, 2, 10), new BigDecimal(icms), new BigDecimal(ipi),
				new BigDecimal(pis), new BigDecimal(cofins), BigDecimal.ZERO);
	}

	private static DocumentTaxRecord nfse(String iss) {
		return new DocumentTaxRecord("NFSE", LocalDate.of(2028, 2, 12), BigDecimal.ZERO, BigDecimal.ZERO,
				BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal(iss));
	}
}
