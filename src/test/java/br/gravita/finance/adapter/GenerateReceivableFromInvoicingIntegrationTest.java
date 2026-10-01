package br.gravita.finance.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import br.gravita.core.domain.finance.Receivable;
import br.gravita.core.domain.finance.ReceivableOrigin;
import br.gravita.core.ports.inbound.finance.GenerateReceivableFromInvoicingCommand;
import br.gravita.core.ports.inbound.finance.GenerateReceivableFromInvoicingCommand.Installment;
import br.gravita.core.ports.inbound.finance.GenerateReceivableFromInvoicingUseCase;
import br.gravita.core.ports.outbound.persistence.finance.ReceivableRepositoryPort;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class GenerateReceivableFromInvoicingIntegrationTest {

	@Autowired
	private GenerateReceivableFromInvoicingUseCase useCase;

	@Autowired
	private ReceivableRepositoryPort receivableRepositoryPort;

	@Test
	@DisplayName("Persists one receivable per installment and does not duplicate them when invoicing again")
	void persistsOneReceivablePerInstallmentAndDoesNotDuplicateOnReinvoicing() {
		UUID documentId = UUID.randomUUID();
		GenerateReceivableFromInvoicingCommand command = new GenerateReceivableFromInvoicingCommand(
				UUID.randomUUID(), documentId,
				List.of(new Installment(LocalDate.now().plusDays(30), new BigDecimal("60.00")),
						new Installment(LocalDate.now().plusDays(60), new BigDecimal("40.00"))));

		List<Receivable> first = useCase.execute(command);
		List<Receivable> second = useCase.execute(command);

		assertThat(first).hasSize(2);
		assertThat(second).extracting(Receivable::getId).containsExactlyElementsOf(
				first.stream().map(Receivable::getId).toList());
		List<Receivable> stored = receivableRepositoryPort.findByOriginDocumentRef(documentId);
		assertThat(stored).hasSize(2);
		assertThat(stored).allSatisfy(r -> assertThat(r.getOrigin()).isEqualTo(ReceivableOrigin.INVOICING));
		assertThat(stored).extracting(Receivable::getInstallmentNumber).containsExactly(1, 2);
	}
}
