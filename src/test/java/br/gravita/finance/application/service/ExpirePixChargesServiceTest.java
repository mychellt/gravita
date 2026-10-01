package br.gravita.finance.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.finance.PixCharge;
import br.gravita.core.domain.finance.PixChargeId;
import br.gravita.core.domain.finance.PixChargeStatus;
import br.gravita.core.domain.finance.ReceivableId;
import br.gravita.core.ports.outbound.persistence.finance.PixChargeRepositoryPort;
import br.gravita.core.usercases.finance.ExpirePixChargesService;
import br.gravita.finance.PixPayloads;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExpirePixChargesServiceTest {

	@Mock
	private PixChargeRepositoryPort pixChargeRepositoryPort;

	@InjectMocks
	private ExpirePixChargesService service;

	private PixCharge pendingCharge() {
		return PixCharge.of(PixChargeId.of(UUID.randomUUID()), ReceivableId.of(UUID.randomUUID()),
				PixPayloads.valid(), BigDecimal.TEN, LocalDate.now(), Instant.now().minusSeconds(60),
				PixChargeStatus.PENDING);
	}

	@Test
	@DisplayName("Expires every pending charge that is past its expiry")
	void expiresEveryPendingChargePastItsExpiry() {
		when(pixChargeRepositoryPort.findPendingExpiredBefore(any()))
				.thenReturn(List.of(pendingCharge(), pendingCharge()));

		int expired = service.execute();

		assertThat(expired).isEqualTo(2);
		ArgumentCaptor<PixCharge> saved = ArgumentCaptor.forClass(PixCharge.class);
		verify(pixChargeRepositoryPort, org.mockito.Mockito.times(2)).save(saved.capture());
		assertThat(saved.getAllValues()).allSatisfy(c -> assertThat(c.getStatus()).isEqualTo(PixChargeStatus.EXPIRED));
	}

	@Test
	@DisplayName("Does nothing when no charge is due to expire")
	void doesNothingWhenNothingIsDue() {
		when(pixChargeRepositoryPort.findPendingExpiredBefore(any())).thenReturn(List.of());

		assertThat(service.execute()).isZero();
		verify(pixChargeRepositoryPort, never()).save(any());
	}
}
