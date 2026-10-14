package br.gravita.core.usercases.tax;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.exceptions.UnauthorizedException;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.system.User;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.domain.tax.InboundNfe;
import br.gravita.core.ports.outbound.persistence.system.UserRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.InboundNfeRepositoryPort;
import br.gravita.core.usercases.CallerCompanyResolver;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListInboundNfeServiceTest {

	@Mock
	private InboundNfeRepositoryPort inboundNfeRepositoryPort;

	@Mock
	private UserRepositoryPort userRepositoryPort;

	private ListInboundNfeService service;

	private final UserId callerId = UserId.generate();

	@BeforeEach
	void setUp() {
		service = new ListInboundNfeService(inboundNfeRepositoryPort, new CallerCompanyResolver(userRepositoryPort));
	}

	@Test
	@DisplayName("Lists only the inbound NF-e of the caller's company")
	void shouldListOnlyTheCallersCompanyInboundNfe() {
		final UUID companyId = UUID.randomUUID();
		when(userRepositoryPort.findById(callerId)).thenReturn(Optional.of(User.builder().companyId(companyId).build()));
		final List<InboundNfe> documents = List.of(mock(InboundNfe.class), mock(InboundNfe.class));
		when(inboundNfeRepositoryPort.findAllByCompany(CompanyId.of(companyId))).thenReturn(documents);

		assertThat(service.execute(new Context().withCaller(callerId))).isEqualTo(documents);
	}

	@Test
	@DisplayName("Returns an empty list, without querying, when the caller has no company")
	void shouldReturnEmptyWhenCallerHasNoCompany() {
		when(userRepositoryPort.findById(callerId)).thenReturn(Optional.of(User.builder().companyId(null).build()));

		assertThat(service.execute(new Context().withCaller(callerId))).isEmpty();
		verify(inboundNfeRepositoryPort, never()).findAllByCompany(any());
	}

	@Test
	@DisplayName("Rejects a request without an authenticated caller")
	void shouldRejectRequestWithoutCaller() {
		assertThatThrownBy(() -> service.execute(new Context())).isInstanceOf(UnauthorizedException.class);
	}
}
