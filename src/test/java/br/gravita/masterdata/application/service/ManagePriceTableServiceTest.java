package br.gravita.masterdata.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.core.ports.inbound.masterdata.UpsertPriceTableCommand;
import br.gravita.core.ports.outbound.persistence.PriceTableRepositoryPort;
import br.gravita.core.usercases.ManagePriceTable;
import br.gravita.core.domain.masterdata.PriceFormation;
import br.gravita.core.domain.masterdata.PriceTable;
import br.gravita.core.domain.masterdata.PriceTableEntry;
import br.gravita.core.domain.masterdata.PriceTableId;
import br.gravita.core.domain.masterdata.PriceTableNotFoundException;
import br.gravita.core.domain.masterdata.ProductOrClassRef;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ManagePriceTableServiceTest {

	private static final ProductOrClassRef PRODUCT_REF = ProductOrClassRef.product(UUID.randomUUID().toString());

	@Mock
	private PriceTableRepositoryPort priceTableRepositoryPort;

	@Test
	void shouldCreateNewPriceTableWhenNoIdIsProvided() {
		ManagePriceTable service = new ManagePriceTable(priceTableRepositoryPort);
		when(priceTableRepositoryPort.save(any(PriceTable.class))).thenAnswer(invocation -> invocation.getArgument(0));

		UpsertPriceTableCommand command = new UpsertPriceTableCommand(null, PriceFormation.FIXED,
				LocalDate.of(2026, 1, 1), null, null, null, List.of(new PriceTableEntry(PRODUCT_REF, BigDecimal.TEN)));

		PriceTableId id = service.execute(command);

		assertThat(id).isNotNull();
		ArgumentCaptor<PriceTable> saved = ArgumentCaptor.forClass(PriceTable.class);
		verify(priceTableRepositoryPort).save(saved.capture());
		assertThat(saved.getValue().getId()).isEqualTo(id);
	}

	@Test
	void shouldUpdateExistingPriceTableWhenIdIsProvidedAndFound() {
		UUID existingId = UUID.randomUUID();
		PriceTable existing = PriceTable.of(PriceTableId.of(existingId), PriceFormation.FIXED, LocalDate.of(2026, 1, 1),
				null, null, null, List.of(new PriceTableEntry(PRODUCT_REF, BigDecimal.TEN)));
		when(priceTableRepositoryPort.findById(PriceTableId.of(existingId))).thenReturn(Optional.of(existing));
		when(priceTableRepositoryPort.save(any(PriceTable.class))).thenAnswer(invocation -> invocation.getArgument(0));

		ManagePriceTable service = new ManagePriceTable(priceTableRepositoryPort);
		UpsertPriceTableCommand command = new UpsertPriceTableCommand(existingId, PriceFormation.PERCENT_OVER_BASE,
				LocalDate.of(2026, 2, 1), null, null, null,
				List.of(new PriceTableEntry(PRODUCT_REF, BigDecimal.valueOf(15))));

		PriceTableId id = service.execute(command);

		assertThat(id.value()).isEqualTo(existingId);
		ArgumentCaptor<PriceTable> saved = ArgumentCaptor.forClass(PriceTable.class);
		verify(priceTableRepositoryPort).save(saved.capture());
		assertThat(saved.getValue().getFormation()).isEqualTo(PriceFormation.PERCENT_OVER_BASE);
	}

	@Test
	void shouldRejectUpdateWhenPriceTableIdIsUnknown() {
		UUID unknownId = UUID.randomUUID();
		when(priceTableRepositoryPort.findById(PriceTableId.of(unknownId))).thenReturn(Optional.empty());

		ManagePriceTable service = new ManagePriceTable(priceTableRepositoryPort);
		UpsertPriceTableCommand command = new UpsertPriceTableCommand(unknownId, PriceFormation.FIXED,
				LocalDate.of(2026, 1, 1), null, null, null, List.of(new PriceTableEntry(PRODUCT_REF, BigDecimal.TEN)));

		assertThatThrownBy(() -> service.execute(command)).isInstanceOf(PriceTableNotFoundException.class);
		verify(priceTableRepositoryPort, never()).save(any());
	}
}
