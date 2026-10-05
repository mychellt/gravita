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
import br.gravita.core.domain.exceptions.UnauthorizedException;
import br.gravita.core.domain.masterdata.Company;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.SefazEnvironment;
import br.gravita.core.domain.masterdata.TaxRegime;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.domain.tax.NfceSale;
import br.gravita.core.domain.tax.NfceSaleId;
import br.gravita.core.domain.tax.NfceSaleStatus;
import br.gravita.core.domain.tax.Payment;
import br.gravita.core.domain.tax.PosSession;
import br.gravita.core.domain.tax.PosSessionId;
import br.gravita.core.domain.tax.SaleItem;
import br.gravita.core.ports.inbound.tax.CancelNfceCommand;
import br.gravita.core.ports.outbound.persistence.CompanyRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.NfceRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.PosSessionRepositoryPort;
import br.gravita.core.ports.outbound.tax.SefazCancellationRequest;
import br.gravita.core.ports.outbound.tax.SefazSubmissionResult;
import br.gravita.core.ports.outbound.tax.SubmitToSefazPort;
import br.gravita.core.ports.outbound.tax.SupervisorAuthorizationPort;
import br.gravita.core.usercases.tax.CancelNfceService;
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
class CancelNfceServiceTest {

	@Mock
	private NfceRepositoryPort nfceRepositoryPort;

	@Mock
	private PosSessionRepositoryPort posSessionRepositoryPort;

	@Mock
	private CompanyRepositoryPort companyRepositoryPort;

	@Mock
	private SupervisorAuthorizationPort supervisorAuthorizationPort;

	@Mock
	private SubmitToSefazPort submitToSefazPort;

	private CancelNfceService service;

	private UUID saleId;
	private PosSessionId sessionId;
	private CompanyId companyId;

	@BeforeEach
	void setUp() {
		service = new CancelNfceService(nfceRepositoryPort, posSessionRepositoryPort, companyRepositoryPort,
				supervisorAuthorizationPort, submitToSefazPort);

		saleId = UUID.randomUUID();
		sessionId = PosSessionId.of(UUID.randomUUID());
		companyId = CompanyId.of(UUID.randomUUID());

		lenient().when(supervisorAuthorizationPort.authorize("super-secret")).thenReturn(true);
		lenient().when(posSessionRepositoryPort.findById(sessionId)).thenReturn(Optional.of(posSession()));
		lenient().when(companyRepositoryPort.findById(companyId)).thenReturn(Optional.of(company()));
		lenient().when(submitToSefazPort.cancel(any())).thenReturn(new SefazSubmissionResult("cancel-protocol-1"));
		lenient().when(nfceRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
	}

	private NfceSale authorizedSale(final Instant createdAt) {
		return authorizedSale(saleId, createdAt);
	}

	private NfceSale authorizedSale(final UUID id, final Instant createdAt) {
		final SaleItem item = new SaleItem(UUID.randomUUID(), BigDecimal.ONE, new BigDecimal("10.00"), null);
		final NfceSale draft = NfceSale.register(NfceSaleId.of(id), sessionId, List.of(item), null,
				List.of(new Payment(PaymentMethodType.CASH, new BigDecimal("10.00"))), null, createdAt);
		return draft.authorize("001", 10L, "3".repeat(44), "issue-protocol-1");
	}

	private PosSession posSession() {
		return PosSession.open(sessionId, UUID.randomUUID(), UUID.randomUUID(), companyId, BigDecimal.ZERO,
				Instant.now());
	}

	private Company company() {
		return Company.builder()
				.id(companyId)
				.name("Acme Ltda")
				.cnpj(Document.cnpj("11.222.333/0001-81"))
				.ie("123456789")
				.im("987654")
				.cnae("6201500")
				.taxRegime(TaxRegime.SIMPLES_NACIONAL)
				.simplesOptante(true)
				.sefazEnvironment(SefazEnvironment.HOMOLOGATION)
				.address("Rua Teste, 100")
				.state("SP")
				.issuingEmail("nfce@example.com")
				.phone("11999999999")
				.logoUrl(null)
				.parentCompanyId(null)
				.build();
	}

	private CancelNfceCommand command() {
		return new CancelNfceCommand(saleId, "super-secret", "customer changed their mind");
	}

	@Test
	@DisplayName("Rejects the cancellation when the supervisor credential is invalid")
	void ac1InvalidSupervisorCredentialIsRejected() {
		when(supervisorAuthorizationPort.authorize("wrong-password")).thenReturn(false);

		assertThatThrownBy(() -> service.execute(new CancelNfceCommand(saleId, "wrong-password", null)))
				.isInstanceOf(UnauthorizedException.class);

		verify(nfceRepositoryPort, never()).findById(any());
		verify(submitToSefazPort, never()).cancel(any());
	}

	@Test
	@DisplayName("Rejects cancelling a sale that is neither the last sale nor from today")
	void ac2ASaleThatIsNeitherTheLastSaleNorFromTodayIsRejected() {
		final Instant threeDaysAgo = Instant.now().minus(java.time.Duration.ofDays(3));
		final NfceSale oldSale = authorizedSale(threeDaysAgo);
		when(nfceRepositoryPort.findById(NfceSaleId.of(saleId))).thenReturn(Optional.of(oldSale));
		when(nfceRepositoryPort.findMostRecent()).thenReturn(Optional.of(authorizedSale(UUID.randomUUID(), Instant.now())));

		assertThatThrownBy(() -> service.execute(command())).isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("not eligible for cancellation");

		verify(submitToSefazPort, never()).cancel(any());
		verify(nfceRepositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("Allows cancelling the last sale even when it is not from today")
	void ac2TheLastSaleIsEligibleEvenIfNotFromToday() {
		final Instant tenMinutesAgo = Instant.now().minus(java.time.Duration.ofMinutes(10));
		final NfceSale sale = authorizedSale(tenMinutesAgo);
		when(nfceRepositoryPort.findById(NfceSaleId.of(saleId))).thenReturn(Optional.of(sale));
		when(nfceRepositoryPort.findMostRecent()).thenReturn(Optional.of(sale));

		service.execute(command());

		verify(submitToSefazPort).cancel(any());
	}

	@Test
	@DisplayName("Rejects cancelling a sale past the thirty-minute window")
	void ac3ASaleThatIsPastTheThirtyMinuteWindowIsRejected() {
		final Instant fortyFiveMinutesAgo = Instant.now().minus(java.time.Duration.ofMinutes(45));
		final NfceSale sale = authorizedSale(fortyFiveMinutesAgo);
		when(nfceRepositoryPort.findById(NfceSaleId.of(saleId))).thenReturn(Optional.of(sale));
		when(nfceRepositoryPort.findMostRecent()).thenReturn(Optional.of(sale));

		assertThatThrownBy(() -> service.execute(command())).isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("Cancellation window has expired");

		verify(submitToSefazPort, never()).cancel(any());
		verify(nfceRepositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("Transmits a successful cancellation to SEFAZ and updates the sale status")
	void ac4SuccessfulCancellationTransmitsToSefazAndUpdatesTheSaleStatus() {
		final Instant fiveMinutesAgo = Instant.now().minus(java.time.Duration.ofMinutes(5));
		final NfceSale sale = authorizedSale(fiveMinutesAgo);
		when(nfceRepositoryPort.findById(NfceSaleId.of(saleId))).thenReturn(Optional.of(sale));
		when(nfceRepositoryPort.findMostRecent()).thenReturn(Optional.of(sale));

		service.execute(command());

		final ArgumentCaptor<SefazCancellationRequest> requestCaptor = ArgumentCaptor.forClass(SefazCancellationRequest.class);
		verify(submitToSefazPort).cancel(requestCaptor.capture());
		assertThat(requestCaptor.getValue().accessKey()).isEqualTo(sale.getAccessKey());
		assertThat(requestCaptor.getValue().protocol()).isEqualTo(sale.getSefazProtocol());
		assertThat(requestCaptor.getValue().reason()).isEqualTo("customer changed their mind");

		final ArgumentCaptor<NfceSale> saveCaptor = ArgumentCaptor.forClass(NfceSale.class);
		verify(nfceRepositoryPort).save(saveCaptor.capture());
		assertThat(saveCaptor.getValue().getStatus()).isEqualTo(NfceSaleStatus.CANCELLED);
	}

	@Test
	@DisplayName("Refuses to cancel a sale that is not authorized")
	void nonAuthorizedSaleCannotBeCancelled() {
		final SaleItem item = new SaleItem(UUID.randomUUID(), BigDecimal.ONE, new BigDecimal("10.00"), null);
		final NfceSale draftSale = NfceSale.register(NfceSaleId.of(saleId), PosSessionId.of(UUID.randomUUID()),
				List.of(item), null, List.of(new Payment(PaymentMethodType.CASH, new BigDecimal("10.00"))), null,
				Instant.now());
		when(nfceRepositoryPort.findById(NfceSaleId.of(saleId))).thenReturn(Optional.of(draftSale));

		assertThatThrownBy(() -> service.execute(command())).isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("not AUTHORIZED");

		verify(submitToSefazPort, never()).cancel(any());
	}

	@Test
	@DisplayName("Rejects cancelling a sale that does not exist")
	void nonExistentSaleIsRejected() {
		when(nfceRepositoryPort.findById(NfceSaleId.of(saleId))).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(command())).isInstanceOf(ResourceNotFoundException.class);

		verify(submitToSefazPort, never()).cancel(any());
	}
}
