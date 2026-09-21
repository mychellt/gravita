package br.gravita.masterdata.domain.model;

import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.DocumentSeries;
import br.gravita.core.domain.masterdata.FiscalDocumentType;
import br.gravita.core.domain.shared.BusinessRuleException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentSeriesTest {

	private static final CompanyId COMPANY_ID = CompanyId.of(UUID.randomUUID());

	@Test
	void shouldAllowAnyNextNumberOnFirstConfiguration() {
		DocumentSeries placeholder = DocumentSeries.placeholder(COMPANY_ID, FiscalDocumentType.NFE);

		DocumentSeries configured = placeholder.reconfigure("001", 500L);

		assertThat(configured.getSeries()).isEqualTo("001");
		assertThat(configured.getNextNumber()).isEqualTo(500L);
	}

	@Test
	void shouldAllowIncreasingNextNumberOnceAlreadyConfigured() {
		DocumentSeries configured = DocumentSeries.placeholder(COMPANY_ID, FiscalDocumentType.NFCE).reconfigure("001", 100L);

		DocumentSeries reconfigured = configured.reconfigure("001", 150L);

		assertThat(reconfigured.getNextNumber()).isEqualTo(150L);
	}

	@Test
	void shouldRejectDecreasingNextNumberOnceAlreadyConfigured() {
		DocumentSeries configured = DocumentSeries.placeholder(COMPANY_ID, FiscalDocumentType.NFSE).reconfigure("001", 100L);

		assertThatThrownBy(() -> configured.reconfigure("001", 50L))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("nextNumber cannot be decreased");
	}

	@Test
	void shouldKeepEachDocumentTypeIndependent() {
		DocumentSeries nfe = DocumentSeries.placeholder(COMPANY_ID, FiscalDocumentType.NFE).reconfigure("001", 100L);
		DocumentSeries nfce = DocumentSeries.placeholder(COMPANY_ID, FiscalDocumentType.NFCE).reconfigure("001", 1L);

		assertThat(nfe.getNextNumber()).isEqualTo(100L);
		assertThat(nfce.getNextNumber()).isEqualTo(1L);
	}

	@Test
	void shouldAdvanceNextNumberByOneOnAllocation() {
		DocumentSeries configured = DocumentSeries.placeholder(COMPANY_ID, FiscalDocumentType.NFE).reconfigure("001", 500L);

		DocumentSeries advanced = configured.allocateNext();

		assertThat(advanced.getNextNumber()).isEqualTo(501L);
		assertThat(advanced.getSeries()).isEqualTo("001");
	}

	@Test
	void shouldRejectAllocationWhenSeriesNotYetConfigured() {
		DocumentSeries placeholder = DocumentSeries.placeholder(COMPANY_ID, FiscalDocumentType.NFE);

		assertThatThrownBy(placeholder::allocateNext)
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("not configured");
	}
}
