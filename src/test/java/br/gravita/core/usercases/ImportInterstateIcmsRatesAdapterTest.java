package br.gravita.core.usercases;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.InterstateIcmsRateDomain;
import br.gravita.core.ports.outbound.persistence.InterstateIcmsRateRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImportInterstateIcmsRatesAdapterTest {

	@Mock
	private InterstateIcmsRateRepositoryPort interstateIcmsRateRepositoryPort;

	@Test
	void shouldAssignNewIdForUnseenStatePair() {
		ImportInterstateIcmsRatesAdapter adapter = new ImportInterstateIcmsRatesAdapter(interstateIcmsRateRepositoryPort);
		InterstateIcmsRateDomain incoming = InterstateIcmsRateDomain.builder()
				.originState("SP").destinationState("RJ").ratePercent(new BigDecimal("12.00")).build();
		when(interstateIcmsRateRepositoryPort.findByOriginStateAndDestinationState("SP", "RJ")).thenReturn(Optional.empty());
		when(interstateIcmsRateRepositoryPort.saveAll(List.of(incoming))).thenAnswer(invocation -> invocation.getArgument(0));

		adapter.execute(new Context(List.of(incoming)));

		assertThat(incoming.getId()).isNotNull();
	}

	@Test
	void shouldUpsertByStatePairSoReimportUpdatesTheExistingRate() {
		ImportInterstateIcmsRatesAdapter adapter = new ImportInterstateIcmsRatesAdapter(interstateIcmsRateRepositoryPort);
		UUID existingId = UUID.randomUUID();
		InterstateIcmsRateDomain incoming = InterstateIcmsRateDomain.builder()
				.originState("SP").destinationState("RJ").ratePercent(new BigDecimal("7.00")).build();
		when(interstateIcmsRateRepositoryPort.findByOriginStateAndDestinationState("SP", "RJ"))
				.thenReturn(Optional.of(InterstateIcmsRateDomain.builder().id(existingId).originState("SP").destinationState("RJ").build()));
		when(interstateIcmsRateRepositoryPort.saveAll(List.of(incoming))).thenAnswer(invocation -> invocation.getArgument(0));

		adapter.execute(new Context(List.of(incoming)));

		assertThat(incoming.getId()).isEqualTo(existingId);
		assertThat(incoming.getRatePercent()).isEqualByComparingTo("7.00");
	}
}
