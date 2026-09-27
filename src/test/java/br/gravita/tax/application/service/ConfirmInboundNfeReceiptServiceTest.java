package br.gravita.tax.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.masterdata.Company;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.SefazEnvironment;
import br.gravita.core.domain.masterdata.TaxRegime;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.domain.tax.InboundNfe;
import br.gravita.core.domain.tax.InboundNfeId;
import br.gravita.core.domain.tax.InboundNfeItem;
import br.gravita.core.domain.tax.InboundNfeNotFoundException;
import br.gravita.core.domain.tax.InboundNfeTotals;
import br.gravita.core.ports.inbound.tax.CalculateTaxCommand;
import br.gravita.core.ports.inbound.tax.CalculateTaxUseCase;
import br.gravita.core.ports.inbound.tax.ConfirmInboundNfeReceiptCommand;
import br.gravita.core.ports.inbound.tax.ConfirmInboundNfeReceiptCommand.ConferenceItem;
import br.gravita.core.ports.inbound.tax.TaxCalculationResult;
import br.gravita.core.ports.outbound.persistence.CompanyRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.InboundNfeRepositoryPort;
import br.gravita.core.ports.outbound.tax.NotifyPayableGeneratedPort;
import br.gravita.core.ports.outbound.tax.NotifyPayableGeneratedPort.NotifyPayableGeneratedCommand;
import br.gravita.core.ports.outbound.tax.NotifyStockEntryPort;
import br.gravita.core.ports.outbound.tax.NotifyStockEntryPort.NotifyStockEntryCommand;
import br.gravita.core.usercases.tax.ConfirmInboundNfeReceiptService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConfirmInboundNfeReceiptServiceTest {

	@Mock
	private InboundNfeRepositoryPort inboundNfeRepositoryPort;

	@Mock
	private CompanyRepositoryPort companyRepositoryPort;

	@Mock
	private CalculateTaxUseCase calculateTaxUseCase;

	@Mock
	private NotifyStockEntryPort notifyStockEntryPort;

	@Mock
	private NotifyPayableGeneratedPort notifyPayableGeneratedPort;

	private ConfirmInboundNfeReceiptService service;

	private final CompanyId companyId = CompanyId.of(UUID.randomUUID());
	private final UUID productId = UUID.randomUUID();
	private final Instant issuedAt = Instant.parse("2026-01-10T12:00:00Z");

	@BeforeEach
	void setUp() {
		service = new ConfirmInboundNfeReceiptService(inboundNfeRepositoryPort, companyRepositoryPort,
				calculateTaxUseCase, notifyStockEntryPort, notifyPayableGeneratedPort);
	}

	@Test
	void confirmingRecordsTheThreeWayComparisonBeforeConfirmingTheReceipt() {
		InboundNfe pending = pendingInboundNfe();
		mockLookups(pending);
		when(inboundNfeRepositoryPort.save(any(InboundNfe.class))).thenAnswer(invocation -> invocation.getArgument(0));

		InboundNfe result = service.execute(command(pending.getId().value(), new BigDecimal("10"), new BigDecimal("8")));

		assertThat(result.getConferenceResult()).hasSize(1);
		assertThat(result.getConferenceResult().get(0).itemRef()).isEqualTo(productId);
		assertThat(result.getConferenceResult().get(0).orderedQty()).isEqualByComparingTo("10");
		assertThat(result.getConferenceResult().get(0).receivedQty()).isEqualByComparingTo("8");
		assertThat(result.getItems().get(0).quantity()).isEqualByComparingTo("10");
	}

	@Test
	void confirmingRegistersTheStockEntryImmediatelyForWhatWasPhysicallyReceived() {
		InboundNfe pending = pendingInboundNfe();
		mockLookups(pending);
		when(inboundNfeRepositoryPort.save(any(InboundNfe.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.execute(command(pending.getId().value(), new BigDecimal("10"), new BigDecimal("8")));

		ArgumentCaptor<NotifyStockEntryCommand> captor = ArgumentCaptor.forClass(NotifyStockEntryCommand.class);
		verify(notifyStockEntryPort).notifyEntry(captor.capture());
		assertThat(captor.getValue().productId()).isEqualTo(productId);
		assertThat(captor.getValue().quantity()).isEqualByComparingTo("8");
		assertThat(captor.getValue().unitCost()).isEqualByComparingTo("1.5000");
		assertThat(captor.getValue().sourceInboundNfeId()).isEqualTo(pending.getId().value());
	}

	@Test
	void confirmingGeneratesAPayableInstallmentMatchingTheNfsOwnTotalValue() {
		InboundNfe pending = pendingInboundNfe();
		mockLookups(pending);
		when(inboundNfeRepositoryPort.save(any(InboundNfe.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.execute(command(pending.getId().value(), new BigDecimal("10"), new BigDecimal("8")));

		ArgumentCaptor<NotifyPayableGeneratedCommand> captor =
				ArgumentCaptor.forClass(NotifyPayableGeneratedCommand.class);
		verify(notifyPayableGeneratedPort).notifyGenerated(captor.capture());
		assertThat(captor.getValue().sourceInboundNfeId()).isEqualTo(pending.getId().value());
		assertThat(captor.getValue().supplierDocument()).isEqualTo("11222333000181");
		assertThat(captor.getValue().installments()).hasSize(1);
		assertThat(captor.getValue().installments().get(0).amount()).isEqualByComparingTo("165.00");
		assertThat(captor.getValue().installments().get(0).dueDate())
				.isEqualTo(issuedAt.atZone(ZoneOffset.UTC).toLocalDate().plusDays(30));
	}

	@Test
	void confirmingComputesTheIcmsPisCofinsCreditUnderTheCompanysTaxRegime() {
		InboundNfe pending = pendingInboundNfe();
		mockLookups(pending);
		when(inboundNfeRepositoryPort.save(any(InboundNfe.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.execute(command(pending.getId().value(), new BigDecimal("10"), new BigDecimal("8")));

		ArgumentCaptor<CalculateTaxCommand> captor = ArgumentCaptor.forClass(CalculateTaxCommand.class);
		verify(calculateTaxUseCase).execute(captor.capture());
		assertThat(captor.getValue().taxRegime()).isEqualTo(br.gravita.core.domain.tax.TaxRegime.LUCRO_PRESUMIDO);
		assertThat(captor.getValue().originState()).isEqualTo("SP");
		assertThat(captor.getValue().destinationState()).isEqualTo("SP");
		assertThat(captor.getValue().items()).hasSize(1);
		assertThat(captor.getValue().items().get(0).productRef()).isEqualTo(productId.toString());
		assertThat(captor.getValue().items().get(0).quantity()).isEqualByComparingTo("10");
	}

	@Test
	void rejectsConfirmingAnInboundNfeThatDoesNotExist() {
		UUID inboundNfeId = UUID.randomUUID();
		when(inboundNfeRepositoryPort.findById(InboundNfeId.of(inboundNfeId))).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(command(inboundNfeId, BigDecimal.TEN, BigDecimal.TEN)))
				.isInstanceOf(InboundNfeNotFoundException.class);

		verify(notifyStockEntryPort, never()).notifyEntry(any());
		verify(notifyPayableGeneratedPort, never()).notifyGenerated(any());
		verify(calculateTaxUseCase, never()).execute(any());
	}

	@Test
	void rejectsConfirmingAnAlreadyConfirmedInboundNfeWithoutDuplicatingSideEffects() {
		InboundNfe confirmed = pendingInboundNfe()
				.confirm(List.of(new br.gravita.core.domain.tax.InboundNfeConferenceItem(productId, BigDecimal.TEN,
						BigDecimal.TEN)));
		when(inboundNfeRepositoryPort.findById(confirmed.getId())).thenReturn(Optional.of(confirmed));

		assertThatThrownBy(() -> service.execute(command(confirmed.getId().value(), BigDecimal.TEN, BigDecimal.TEN)))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("already confirmed");

		verify(notifyStockEntryPort, never()).notifyEntry(any());
		verify(notifyPayableGeneratedPort, never()).notifyGenerated(any());
		verify(calculateTaxUseCase, never()).execute(any());
		verify(inboundNfeRepositoryPort, never()).save(any());
	}

	private void mockLookups(InboundNfe pending) {
		when(inboundNfeRepositoryPort.findById(pending.getId())).thenReturn(Optional.of(pending));
		when(companyRepositoryPort.findById(companyId)).thenReturn(Optional.of(company()));
		when(calculateTaxUseCase.execute(any())).thenReturn(new TaxCalculationResult(List.of(), null));
	}

	private ConfirmInboundNfeReceiptCommand command(UUID inboundNfeId, BigDecimal orderedQty, BigDecimal receivedQty) {
		return new ConfirmInboundNfeReceiptCommand(inboundNfeId,
				List.of(new ConferenceItem(productId, orderedQty, receivedQty)));
	}

	private InboundNfe pendingInboundNfe() {
		return InboundNfe.importedFromXml(InboundNfeId.of(UUID.randomUUID()), companyId,
				"35240111222333000181550010000012345123456789", "1", "12345", Document.cnpj("11222333000181"),
				"Fornecedor Exemplo LTDA", issuedAt, List.of(item()), totals(), "xml-ref-1");
	}

	private InboundNfeItem item() {
		return new InboundNfeItem("SKU-001", "Parafuso Sextavado M8", "73181500", "5102", "UN",
				new BigDecimal("10"), new BigDecimal("1.5000"), new BigDecimal("150.00"), new BigDecimal("27.00"),
				BigDecimal.ZERO, new BigDecimal("2.48"), new BigDecimal("11.40"));
	}

	private InboundNfeTotals totals() {
		return new InboundNfeTotals(new BigDecimal("150.00"), new BigDecimal("15.00"), BigDecimal.ZERO,
				BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("27.00"), BigDecimal.ZERO, new BigDecimal("2.48"),
				new BigDecimal("11.40"), new BigDecimal("165.00"));
	}

	private Company company() {
		return Company.of(companyId, Document.cnpj("11444777000161"), "123456789", "12345", "4711301",
				TaxRegime.LUCRO_PRESUMIDO, false, SefazEnvironment.HOMOLOGATION, "Rua Exemplo, 100", "SP",
				"fiscal@exemplo.com", "11999999999", null, null);
	}
}
