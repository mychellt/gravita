package br.gravita.tax.application.service;

import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.domain.tax.InboundNfe;
import br.gravita.core.domain.tax.InboundNfeItem;
import br.gravita.core.domain.tax.InboundNfeStatus;
import br.gravita.core.domain.tax.InboundNfeTotals;
import br.gravita.core.ports.inbound.tax.EnterInboundNfeManuallyCommand;
import br.gravita.core.ports.inbound.tax.ManualInboundNfeData;
import br.gravita.core.ports.outbound.persistence.tax.InboundNfeRepositoryPort;
import br.gravita.core.usercases.tax.EnterInboundNfeManuallyService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnterInboundNfeManuallyServiceTest {

	private static final String ACCESS_KEY = "35240111222333000181550010000012345123456789";

	@Mock
	private InboundNfeRepositoryPort inboundNfeRepositoryPort;

	private EnterInboundNfeManuallyService service;

	@BeforeEach
	void setUp() {
		service = new EnterInboundNfeManuallyService(inboundNfeRepositoryPort);
	}

	@Test
	@DisplayName("Succeeds with fully manual data and no top-level access key")
	void fullyManualDataSucceedsWithNoAccessKeyGivenAtTheTopLevel() {
		final CompanyId companyId = CompanyId.of(UUID.randomUUID());
		when(inboundNfeRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		final InboundNfe result = service
				.execute(new EnterInboundNfeManuallyCommand(companyId, null, manualData()));

		assertThat(result.getAccessKey()).isEqualTo(ACCESS_KEY);
		assertThat(result.getCompanyId()).isEqualTo(companyId);
		assertThat(result.getSupplierName()).isEqualTo("Fornecedor Exemplo LTDA");
	}

	@Test
	@DisplayName("Produces an inbound NF-e in the same pending-conference shape as an XML import")
	void producesAnInboundNfeInTheSamePendingConferenceShapeAsXmlImport() {
		final CompanyId companyId = CompanyId.of(UUID.randomUUID());
		when(inboundNfeRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		final InboundNfe result = service
				.execute(new EnterInboundNfeManuallyCommand(companyId, ACCESS_KEY, manualData()));

		assertThat(result.getStatus()).isEqualTo(InboundNfeStatus.PENDING_CONFERENCE);
		assertThat(result.getXmlStorageRef()).isNotBlank();
		assertThat(result.getItems()).hasSize(1);
		assertThat(result.getTotals().totalValue()).isEqualByComparingTo("165.00");
	}

	@Test
	@DisplayName("Rejects an access key alone without resolvable manual data")
	void anAccessKeyAloneWithoutResolvableManualDataIsRejected() {
		final CompanyId companyId = CompanyId.of(UUID.randomUUID());

		assertThatThrownBy(() -> service.execute(new EnterInboundNfeManuallyCommand(companyId, ACCESS_KEY, null)))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining(ACCESS_KEY);
	}

	@Test
	@DisplayName("Rejects a request with neither an access key nor manual data")
	void neitherAnAccessKeyNorManualDataIsRejected() {
		final CompanyId companyId = CompanyId.of(UUID.randomUUID());

		assertThatThrownBy(() -> service.execute(new EnterInboundNfeManuallyCommand(companyId, null, null)))
				.isInstanceOf(BusinessRuleException.class);
	}

	private ManualInboundNfeData manualData() {
		return new ManualInboundNfeData(ACCESS_KEY, "1", "12345", Document.cnpj("11222333000181"),
				"Fornecedor Exemplo LTDA", Instant.now(), List.of(item()), totals());
	}

	private InboundNfeItem item() {
		return new InboundNfeItem("SKU-001", "Parafuso Sextavado M8", "73181500", "5102", "UN",
				new BigDecimal("100.0000"), new BigDecimal("1.5000"), new BigDecimal("150.00"),
				new BigDecimal("27.00"), BigDecimal.ZERO, new BigDecimal("2.48"), new BigDecimal("11.40"));
	}

	private InboundNfeTotals totals() {
		return new InboundNfeTotals(new BigDecimal("150.00"), new BigDecimal("15.00"), BigDecimal.ZERO,
				BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("27.00"), BigDecimal.ZERO, new BigDecimal("2.48"),
				new BigDecimal("11.40"), new BigDecimal("165.00"));
	}
}
