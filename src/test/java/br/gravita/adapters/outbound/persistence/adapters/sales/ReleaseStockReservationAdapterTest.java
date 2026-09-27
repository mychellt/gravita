package br.gravita.adapters.outbound.persistence.adapters.sales;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import br.gravita.core.ports.inbound.inventory.ReleaseStockReservationCommand;
import br.gravita.core.ports.inbound.inventory.ReleaseStockReservationUseCase;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReleaseStockReservationAdapterTest {

	@Mock
	private ReleaseStockReservationUseCase releaseStockReservationUseCase;

	@Test
	void delegatesToTheRealUseCaseByOrderRef() {
		ReleaseStockReservationAdapter adapter = new ReleaseStockReservationAdapter(releaseStockReservationUseCase);
		UUID orderId = UUID.randomUUID();

		adapter.releaseByOrderRef(orderId);

		ArgumentCaptor<ReleaseStockReservationCommand> captor =
				ArgumentCaptor.forClass(ReleaseStockReservationCommand.class);
		verify(releaseStockReservationUseCase).execute(captor.capture());

		ReleaseStockReservationCommand command = captor.getValue();
		assertThat(command.orderRef()).isEqualTo(orderId);
		assertThat(command.reservationId()).isNull();
	}
}
