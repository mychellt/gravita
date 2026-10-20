package br.gravita.tax.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.system.User;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.domain.tax.PosSession;
import br.gravita.core.domain.tax.PosSessionId;
import br.gravita.core.domain.tax.PosSessionStatus;
import br.gravita.core.ports.outbound.persistence.system.UserRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.PosSessionRepositoryPort;
import br.gravita.core.usercases.CallerCompanyResolver;
import br.gravita.core.usercases.tax.GetOrOpenPosSessionService;
import java.math.BigDecimal;
import java.time.Instant;
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
class GetOrOpenPosSessionServiceTest {

	@Mock
	private PosSessionRepositoryPort posSessionRepositoryPort;
	@Mock
	private UserRepositoryPort userRepositoryPort;

	private GetOrOpenPosSessionService service;
	private final UserId callerId = UserId.generate();
	private final UUID companyId = UUID.randomUUID();

	@BeforeEach
	void setUp() {
		service = new GetOrOpenPosSessionService(posSessionRepositoryPort, new CallerCompanyResolver(userRepositoryPort));
	}

	@Test
	@DisplayName("Opens the company's register session, with the caller as operator, when none is open")
	void opensASessionWhenNoneIsOpen() {
		when(userRepositoryPort.findById(callerId)).thenReturn(Optional.of(User.builder().companyId(companyId).build()));
		when(posSessionRepositoryPort.findByRegisterIdAndStatus(any(), any())).thenReturn(Optional.empty());
		when(posSessionRepositoryPort.save(any(PosSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

		final PosSessionId id = service.execute(new Context().withCaller(callerId));

		final ArgumentCaptor<PosSession> saved = ArgumentCaptor.forClass(PosSession.class);
		verify(posSessionRepositoryPort).save(saved.capture());
		assertThat(saved.getValue().getId()).isEqualTo(id);
		assertThat(saved.getValue().getOperatorId()).isEqualTo(callerId.value());
		assertThat(saved.getValue().getCompanyId()).isEqualTo(CompanyId.of(companyId));
		assertThat(saved.getValue().getStatus()).isEqualTo(PosSessionStatus.OPEN);
	}

	@Test
	@DisplayName("Reuses the open session of the same register instead of opening another")
	void reusesTheOpenSession() {
		final PosSession open = PosSession.open(PosSessionId.of(UUID.randomUUID()), UUID.randomUUID(), callerId.value(),
				CompanyId.of(companyId), BigDecimal.ZERO, Instant.now());
		when(userRepositoryPort.findById(callerId)).thenReturn(Optional.of(User.builder().companyId(companyId).build()));
		when(posSessionRepositoryPort.findByRegisterIdAndStatus(any(), any())).thenReturn(Optional.of(open));

		assertThat(service.execute(new Context().withCaller(callerId))).isEqualTo(open.getId());
		verify(posSessionRepositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("A user without a company cannot open a register")
	void rejectsAUserWithoutACompany() {
		when(userRepositoryPort.findById(callerId)).thenReturn(Optional.of(User.builder().build()));

		assertThatThrownBy(() -> service.execute(new Context().withCaller(callerId))).isInstanceOf(BusinessRuleException.class);
	}
}
