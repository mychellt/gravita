package br.gravita.tax.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.PaymentMethodType;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.MaxDiscountBehavior;
import br.gravita.core.domain.masterdata.PriceFormation;
import br.gravita.core.domain.masterdata.PriceTable;
import br.gravita.core.domain.masterdata.PriceTableId;
import br.gravita.core.domain.tax.NfceSale;
import br.gravita.core.domain.tax.NfceSaleId;
import br.gravita.core.domain.tax.PosSession;
import br.gravita.core.domain.tax.PosSessionId;
import br.gravita.core.domain.tax.PosSessionStatus;
import br.gravita.core.ports.inbound.tax.RegisterNfceSaleCommand;
import br.gravita.core.ports.inbound.tax.RegisterNfceSaleCommand.PaymentCommand;
import br.gravita.core.ports.inbound.tax.RegisterNfceSaleCommand.SaleItemCommand;
import br.gravita.core.ports.outbound.persistence.PriceTableRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.NfceRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.PosSessionRepositoryPort;
import br.gravita.core.usercases.tax.RegisterNfceSaleService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RegisterNfceSaleServiceTest {

	@Mock
	private PosSessionRepositoryPort posSessionRepositoryPort;

	@Mock
	private NfceRepositoryPort nfceRepositoryPort;

	@Mock
	private PriceTableRepositoryPort priceTableRepositoryPort;

	private RegisterNfceSaleService service;

	private UUID sessionId;

	@BeforeEach
	void setUp() {
		service = new RegisterNfceSaleService(posSessionRepositoryPort, nfceRepositoryPort, priceTableRepositoryPort);
		sessionId = UUID.randomUUID();
		lenient().when(posSessionRepositoryPort.findById(PosSessionId.of(sessionId)))
				.thenReturn(Optional.of(openSession()));
		lenient().when(nfceRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
	}

	private PosSession openSession() {
		return PosSession.of(PosSessionId.of(sessionId), UUID.randomUUID(), UUID.randomUUID(),
				CompanyId.of(UUID.randomUUID()), new BigDecimal("100.00"), PosSessionStatus.OPEN, Instant.now(), null);
	}

	private RegisterNfceSaleCommand commandWithItemDiscount(BigDecimal itemDiscount, UUID priceTableId) {
		SaleItemCommand item = new SaleItemCommand(UUID.randomUUID(), BigDecimal.ONE, new BigDecimal("100.00"),
				itemDiscount);
		PaymentCommand payment = new PaymentCommand(PaymentMethodType.CASH,
				new BigDecimal("100.00").subtract(itemDiscount == null ? BigDecimal.ZERO : itemDiscount));
		return new RegisterNfceSaleCommand(sessionId, List.of(item), null, List.of(payment), null, priceTableId);
	}

	private PriceTable priceTableWithMaxDiscount(String maxDiscountPercent, MaxDiscountBehavior behavior) {
		return PriceTable.of(PriceTableId.of(UUID.randomUUID()), PriceFormation.FIXED, java.time.LocalDate.now(), null,
				new BigDecimal(maxDiscountPercent), behavior, List.of());
	}

	@Test
	@DisplayName("Registers a cart item and produces a draft sale")
	void ac1_registersACartItemAndProducesADraftSale() {
		NfceSaleId id = service.execute(commandWithItemDiscount(null, null));

		ArgumentCaptor<NfceSale> captor = ArgumentCaptor.forClass(NfceSale.class);
		verify(nfceRepositoryPort).save(captor.capture());
		assertThat(captor.getValue().getItems()).hasSize(1);
		assertThat(id).isEqualTo(captor.getValue().getId());
	}

	@Test
	@DisplayName("Accepts an item discount within the linked price table's cap")
	void ac2_anItemDiscountWithinTheLinkedPriceTablesCapIsAccepted() {
		UUID priceTableId = UUID.randomUUID();
		when(priceTableRepositoryPort.findById(PriceTableId.of(priceTableId)))
				.thenReturn(Optional.of(priceTableWithMaxDiscount("20", MaxDiscountBehavior.BLOCK)));

		service.execute(commandWithItemDiscount(new BigDecimal("10.00"), priceTableId));

		verify(nfceRepositoryPort).save(any());
	}

	@Test
	@DisplayName("Rejects an item discount exceeding a blocking price table's cap")
	void ac2_anItemDiscountExceedingABlockPriceTablesCapIsRejected() {
		UUID priceTableId = UUID.randomUUID();
		when(priceTableRepositoryPort.findById(PriceTableId.of(priceTableId)))
				.thenReturn(Optional.of(priceTableWithMaxDiscount("5", MaxDiscountBehavior.BLOCK)));

		assertThatThrownBy(() -> service.execute(commandWithItemDiscount(new BigDecimal("10.00"), priceTableId)))
				.isInstanceOf(br.gravita.core.domain.shared.BusinessRuleException.class);

		verify(nfceRepositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("Still creates the sale when an item discount exceeds only an alerting price table's cap")
	void ac2_anItemDiscountExceedingAnAlertPriceTablesCapStillCreatesTheSale() {
		UUID priceTableId = UUID.randomUUID();
		when(priceTableRepositoryPort.findById(PriceTableId.of(priceTableId)))
				.thenReturn(Optional.of(priceTableWithMaxDiscount("5", MaxDiscountBehavior.ALERT)));

		NfceSaleId id = service.execute(commandWithItemDiscount(new BigDecimal("10.00"), priceTableId));

		assertThat(id).isNotNull();
		verify(nfceRepositoryPort).save(any());
	}

	@Test
	@DisplayName("Combines multiple payment methods in the same sale")
	void ac3_multiplePaymentMethodsAreCombinedInTheSameSale() {
		SaleItemCommand item = new SaleItemCommand(UUID.randomUUID(), BigDecimal.ONE, new BigDecimal("80.00"), null);
		RegisterNfceSaleCommand command = new RegisterNfceSaleCommand(sessionId, List.of(item), null,
				List.of(new PaymentCommand(PaymentMethodType.CASH, new BigDecimal("50.00")),
						new PaymentCommand(PaymentMethodType.CREDIT_CARD, new BigDecimal("30.00"))),
				null, null);

		service.execute(command);

		ArgumentCaptor<NfceSale> captor = ArgumentCaptor.forClass(NfceSale.class);
		verify(nfceRepositoryPort).save(captor.capture());
		assertThat(captor.getValue().getPayments()).hasSize(2);
	}

	@Test
	@DisplayName("Derives the change given from the payments minus the sale total")
	void ac4_changeGivenIsDerivedFromPaymentsMinusSaleTotal() {
		SaleItemCommand item = new SaleItemCommand(UUID.randomUUID(), BigDecimal.ONE, new BigDecimal("18.00"), null);
		RegisterNfceSaleCommand command = new RegisterNfceSaleCommand(sessionId, List.of(item), null,
				List.of(new PaymentCommand(PaymentMethodType.CASH, new BigDecimal("20.00"))), null, null);

		service.execute(command);

		ArgumentCaptor<NfceSale> captor = ArgumentCaptor.forClass(NfceSale.class);
		verify(nfceRepositoryPort).save(captor.capture());
		assertThat(captor.getValue().getChangeGiven()).isEqualByComparingTo("2.00");
	}

	@Test
	@DisplayName("Rejects payments that do not cover the sale total")
	void ac5_paymentsThatDoNotCoverTheSaleTotalAreRejected() {
		SaleItemCommand item = new SaleItemCommand(UUID.randomUUID(), BigDecimal.ONE, new BigDecimal("50.00"), null);
		RegisterNfceSaleCommand command = new RegisterNfceSaleCommand(sessionId, List.of(item), null,
				List.of(new PaymentCommand(PaymentMethodType.CASH, new BigDecimal("40.00"))), null, null);

		assertThatThrownBy(() -> service.execute(command)).isInstanceOf(BusinessRuleException.class);

		verify(nfceRepositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("Records a typed customer CPF on the sale")
	void ac6_aTypedCustomerCpfIsRecordedOnTheSale() {
		SaleItemCommand item = new SaleItemCommand(UUID.randomUUID(), BigDecimal.ONE, new BigDecimal("10.00"), null);
		RegisterNfceSaleCommand command = new RegisterNfceSaleCommand(sessionId, List.of(item), null,
				List.of(new PaymentCommand(PaymentMethodType.CASH, new BigDecimal("10.00"))), "529.982.247-25", null);

		service.execute(command);

		ArgumentCaptor<NfceSale> captor = ArgumentCaptor.forClass(NfceSale.class);
		verify(nfceRepositoryPort).save(captor.capture());
		assertThat(captor.getValue().getCustomerCpf()).isEqualTo("52998224725");
	}

	@Test
	@DisplayName("Rejects a sale for a non-existent session")
	void aNonExistentSessionIsRejected() {
		UUID unknownSession = UUID.randomUUID();
		when(posSessionRepositoryPort.findById(PosSessionId.of(unknownSession))).thenReturn(Optional.empty());
		SaleItemCommand item = new SaleItemCommand(UUID.randomUUID(), BigDecimal.ONE, new BigDecimal("10.00"), null);
		RegisterNfceSaleCommand command = new RegisterNfceSaleCommand(unknownSession, List.of(item), null,
				List.of(new PaymentCommand(PaymentMethodType.CASH, new BigDecimal("10.00"))), null, null);

		assertThatThrownBy(() -> service.execute(command)).isInstanceOf(ResourceNotFoundException.class);

		verify(nfceRepositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("Rejects a sale for a closed session")
	void aClosedSessionIsRejected() {
		PosSession closed = PosSession.of(PosSessionId.of(sessionId), UUID.randomUUID(), UUID.randomUUID(),
				CompanyId.of(UUID.randomUUID()), new BigDecimal("100.00"), PosSessionStatus.CLOSED, Instant.now(),
				Instant.now());
		when(posSessionRepositoryPort.findById(PosSessionId.of(sessionId))).thenReturn(Optional.of(closed));

		assertThatThrownBy(() -> service.execute(commandWithItemDiscount(null, null)))
				.isInstanceOf(BusinessRuleException.class);

		verify(nfceRepositoryPort, never()).save(any());
	}
}
