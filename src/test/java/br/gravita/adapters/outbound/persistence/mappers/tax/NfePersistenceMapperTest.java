package br.gravita.adapters.outbound.persistence.mappers.tax;

import br.gravita.core.domain.PaymentMethodType;
import static org.assertj.core.api.Assertions.assertThat;

import br.gravita.adapters.outbound.persistence.entities.tax.NfeJpaEntity;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.shared.PersonRef;
import br.gravita.core.domain.shared.PersonType;
import br.gravita.core.domain.tax.Cfop;
import br.gravita.core.domain.tax.ItemTaxBreakdown;
import br.gravita.core.domain.tax.NaturezaOperacao;
import br.gravita.core.domain.tax.NfeDocument;
import br.gravita.core.domain.tax.NfeDocumentId;
import br.gravita.core.domain.tax.NfeItem;
import br.gravita.core.domain.tax.NfeRecipient;
import br.gravita.core.domain.tax.TaxCalculationTotals;
import br.gravita.core.domain.tax.TaxLineBreakdown;
import br.gravita.core.domain.tax.TaxType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NfePersistenceMapperTest {

	private final NfePersistenceMapper mapper = new NfePersistenceMapperImpl();

	@Test
	@DisplayName("Maps the payment method id to the entity and back")
	void shouldRoundTripPaymentMethodId() {
		final PaymentMethodType paymentMethodType = PaymentMethodType.BOLETO;

		final NfeJpaEntity entity = mapper.map(document(paymentMethodType));
		final NfeDocument roundTripped = mapper.map(entity);

		assertThat(entity.getPaymentMethodType()).isEqualTo(paymentMethodType);
		assertThat(roundTripped.getPaymentMethodType()).isEqualTo(paymentMethodType);
	}

	@Test
	@DisplayName("Keeps the payment method id null when none was chosen")
	void shouldKeepPaymentMethodIdNull() {
		final NfeJpaEntity entity = mapper.map(document(null));

		assertThat(entity.getPaymentMethodType()).isNull();
		assertThat(mapper.map(entity).getPaymentMethodType()).isNull();
	}

	private NfeDocument document(final PaymentMethodType paymentMethodType) {
		final UUID productId = UUID.randomUUID();
		final TaxLineBreakdown icms = new TaxLineBreakdown(TaxType.ICMS, new BigDecimal("100.00"), new BigDecimal("18"),
				new BigDecimal("18.00"), new BigDecimal("18.00"), false, null);
		final ItemTaxBreakdown breakdown = new ItemTaxBreakdown(0, productId.toString(), List.of(icms));
		final NfeItem item = new NfeItem(productId, "Produto Teste", BigDecimal.ONE, new BigDecimal("100.00"),
				BigDecimal.ZERO, breakdown);
		return NfeDocument.draft().id(NfeDocumentId.of(UUID.randomUUID()))
				.issuerCompanyId(CompanyId.of(UUID.randomUUID())).naturezaOperacao(NaturezaOperacao.VENDA)
				.cfop(new Cfop("5102"))
				.recipient(NfeRecipient.of(PersonRef.of(UUID.randomUUID()), "11.222.333/0001-81", PersonType.COMPANY,
						"Cliente PJ Teste", "123456789", "RJ"))
				.items(List.of(item)).freight(BigDecimal.ZERO).insurance(BigDecimal.ZERO).otherExpenses(BigDecimal.ZERO)
				.paymentMethodType(paymentMethodType).taxTotals(TaxCalculationTotals.from(List.of(breakdown)))
				.createdAt(Instant.parse("2028-02-01T10:00:00Z")).build();
	}
}
