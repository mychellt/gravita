package br.gravita.finance.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.finance.Payable;
import br.gravita.core.domain.finance.PayableId;
import br.gravita.core.domain.finance.PayableOrigin;
import br.gravita.core.domain.finance.PayableStatus;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.domain.system.ApprovalAlcada;
import br.gravita.core.domain.system.ApprovalModule;
import br.gravita.core.domain.system.User;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.domain.system.UserNotFoundException;
import br.gravita.core.ports.inbound.finance.ApprovePayableCommand;
import br.gravita.core.ports.outbound.persistence.finance.PayableRepositoryPort;
import br.gravita.core.ports.outbound.persistence.system.ApprovalAlcadaRepositoryPort;
import br.gravita.core.ports.outbound.persistence.system.UserRepositoryPort;
import br.gravita.core.usercases.finance.ApprovePayableService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApprovePayableServiceTest {

	private static final UUID ELEVATED_PROFILE = UUID.randomUUID();

	@Mock
	private PayableRepositoryPort payableRepositoryPort;

	@Mock
	private ApprovalAlcadaRepositoryPort approvalAlcadaRepositoryPort;

	@Mock
	private UserRepositoryPort userRepositoryPort;

	@InjectMocks
	private ApprovePayableService service;

	private Payable openPayable(String amount) {
		Payable payable = Payable.createManual(PayableId.of(UUID.randomUUID()), null, new BigDecimal(amount),
				LocalDate.now().plusDays(10), null);
		when(payableRepositoryPort.findById(payable.getId())).thenReturn(Optional.of(payable));
		return payable;
	}

	private void alcada(String thresholdValue) {
		when(approvalAlcadaRepositoryPort.findByModule(ApprovalModule.FINANCE))
				.thenReturn(Optional.of(ApprovalAlcada.builder().id(UUID.randomUUID())
						.thresholdValue(thresholdValue == null ? null : new BigDecimal(thresholdValue))
						.approverProfileId(ELEVATED_PROFILE).build()));
	}

	private UUID approverWithProfile(UUID profileId) {
		UUID approver = UUID.randomUUID();
		when(userRepositoryPort.findById(UserId.of(approver)))
				.thenReturn(Optional.of(User.builder().id(UserId.of(approver)).profileId(profileId).build()));
		return approver;
	}

	private void savesWhatItIsGiven() {
		when(payableRepositoryPort.save(any(Payable.class))).thenAnswer(invocation -> invocation.getArgument(0));
	}

	@Test
	@DisplayName("Approves an open payable when no approval limit is configured")
	void approvesAnOpenPayableWhenNoAlcadaIsConfigured() {
		Payable payable = openPayable("5000.00");
		UUID approver = UUID.randomUUID();
		when(approvalAlcadaRepositoryPort.findByModule(ApprovalModule.FINANCE)).thenReturn(Optional.empty());
		savesWhatItIsGiven();

		Payable result = service.execute(new ApprovePayableCommand(payable.getId().value(), approver));

		assertThat(result.getStatus()).isEqualTo(PayableStatus.APPROVED);
		assertThat(result.getApprovedBy()).isEqualTo(approver);
		ArgumentCaptor<Payable> saved = ArgumentCaptor.forClass(Payable.class);
		verify(payableRepositoryPort).save(saved.capture());
		assertThat(saved.getValue().getStatus()).isEqualTo(PayableStatus.APPROVED);
		assertThat(saved.getValue().getApprovedBy()).isEqualTo(approver);
		verify(userRepositoryPort, never()).findById(any());
	}

	@Test
	@DisplayName("Lets any approver approve a payable below the approval limit")
	void anyApproverMayApproveAPayableBelowTheAlcada() {
		Payable payable = openPayable("99.99");
		UUID approver = UUID.randomUUID();
		alcada("100.00");
		savesWhatItIsGiven();

		Payable result = service.execute(new ApprovePayableCommand(payable.getId().value(), approver));

		assertThat(result.getStatus()).isEqualTo(PayableStatus.APPROVED);
		verify(userRepositoryPort, never()).findById(any());
	}

	@Test
	@DisplayName("Does not restrict payables when the approval limit has no value threshold")
	void anAlcadaWithoutAValueThresholdDoesNotRestrictPayables() {
		Payable payable = openPayable("1000000.00");
		alcada(null);
		savesWhatItIsGiven();

		Payable result = service.execute(new ApprovePayableCommand(payable.getId().value(), UUID.randomUUID()));

		assertThat(result.getStatus()).isEqualTo(PayableStatus.APPROVED);
	}

	@Test
	@DisplayName("Requires an approver with the elevated profile for a payable at or above the approval limit")
	void aPayableAtOrAboveTheAlcadaRequiresAnApproverWithTheElevatedProfile() {
		Payable payable = openPayable("100.00");
		alcada("100.00");
		UUID approver = approverWithProfile(ELEVATED_PROFILE);
		savesWhatItIsGiven();

		Payable result = service.execute(new ApprovePayableCommand(payable.getId().value(), approver));

		assertThat(result.getStatus()).isEqualTo(PayableStatus.APPROVED);
		assertThat(result.getApprovedBy()).isEqualTo(approver);
	}

	@Test
	@DisplayName("Rejects an approver without the elevated profile when the payable exceeds the approval limit")
	void rejectsAnApproverWithoutTheElevatedProfileWhenThePayableExceedsTheAlcada() {
		Payable payable = openPayable("250.00");
		alcada("100.00");
		UUID approver = approverWithProfile(UUID.randomUUID());

		assertThatThrownBy(() -> service.execute(new ApprovePayableCommand(payable.getId().value(), approver)))
				.isInstanceOf(BusinessRuleException.class).hasMessageContaining("alcada");

		verify(payableRepositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("Rejects an unknown approver when the payable exceeds the approval limit")
	void rejectsAnUnknownApproverWhenThePayableExceedsTheAlcada() {
		Payable payable = openPayable("250.00");
		alcada("100.00");
		UUID approver = UUID.randomUUID();
		when(userRepositoryPort.findById(UserId.of(approver))).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(new ApprovePayableCommand(payable.getId().value(), approver)))
				.isInstanceOf(UserNotFoundException.class);

		verify(payableRepositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("Rejects approving a payable that does not exist")
	void rejectsAPayableThatDoesNotExist() {
		UUID payableId = UUID.randomUUID();
		when(payableRepositoryPort.findById(PayableId.of(payableId))).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(new ApprovePayableCommand(payableId, UUID.randomUUID())))
				.isInstanceOf(ResourceNotFoundException.class);

		verify(payableRepositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("Rejects approving a payable that is not open")
	void rejectsAPayableThatIsNotOpen() {
		for (PayableStatus status : List.of(PayableStatus.APPROVED, PayableStatus.PAID, PayableStatus.CANCELLED)) {
			Payable payable = Payable.of(PayableId.of(UUID.randomUUID()), null, PayableOrigin.MANUAL, BigDecimal.TEN,
					LocalDate.now().plusDays(3), List.of(), status, null, null, null);
			when(payableRepositoryPort.findById(payable.getId())).thenReturn(Optional.of(payable));

			assertThatThrownBy(
					() -> service.execute(new ApprovePayableCommand(payable.getId().value(), UUID.randomUUID())))
					.isInstanceOf(BusinessRuleException.class);
		}

		verify(payableRepositoryPort, never()).save(any());
	}
}
