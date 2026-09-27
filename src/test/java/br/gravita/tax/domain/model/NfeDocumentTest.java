package br.gravita.tax.domain.model;

import br.gravita.core.domain.IeIndicator;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.domain.tax.ItemTaxBreakdown;
import br.gravita.core.domain.tax.NfeDocument;
import br.gravita.core.domain.tax.NfeDocumentId;
import br.gravita.core.domain.tax.NfeDocumentStatus;
import br.gravita.core.domain.tax.NfeItem;
import br.gravita.core.domain.tax.NfeRecipient;
import br.gravita.core.domain.tax.TaxCalculationTotals;
import br.gravita.core.domain.tax.TaxType;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NfeDocumentTest {

	private static final CompanyId COMPANY_ID = CompanyId.of(UUID.randomUUID());
	private static final String ACCESS_KEY = "35240111222333000181550010000012345123456789";

	@Test
	void draftingStartsPendingNumberingInDraft() {
		NfeDocument nfeDocument = draft("Venda de mercadoria", null);

		assertThat(nfeDocument.getStatus()).isEqualTo(NfeDocumentStatus.DRAFT);
		assertThat(nfeDocument.getSeries()).isNull();
		assertThat(nfeDocument.getAccessKey()).isNull();
		assertThat(nfeDocument.getItems()).hasSize(1);
	}

	@Test
	void queuingAllocatesNumberingAndMovesToQueued() {
		NfeDocument queued = draft("Venda de mercadoria", null).queue("1", 42L, ACCESS_KEY);

		assertThat(queued.getStatus()).isEqualTo(NfeDocumentStatus.QUEUED);
		assertThat(queued.getSeries()).isEqualTo("1");
		assertThat(queued.getNumber()).isEqualTo(42L);
		assertThat(queued.getAccessKey()).isEqualTo(ACCESS_KEY);
	}

	@Test
	void anAlreadyQueuedDocumentCannotBeQueuedAgain() {
		NfeDocument queued = draft("Venda de mercadoria", null).queue("1", 42L, ACCESS_KEY);

		assertThatThrownBy(() -> queued.queue("1", 43L, ACCESS_KEY)).isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void aReturnNatureWithoutAReferencedAccessKeyIsRejected() {
		assertThatThrownBy(() -> draft("Devolucao de compra", null)).isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("referencedAccessKey");
	}

	@Test
	void aComplementaryNoteWithoutAReferencedAccessKeyIsRejected() {
		assertThatThrownBy(() -> draft("Nota complementar", null)).isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("referencedAccessKey");
	}

	@Test
	void aReturnNatureWithAReferencedAccessKeySucceeds() {
		NfeDocument nfeDocument = draft("Devolucao de compra", ACCESS_KEY);

		assertThat(nfeDocument.getReferencedAccessKey()).isEqualTo(ACCESS_KEY);
	}

	@Test
	void rejectsAnEmptyItemList() {
		NfeRecipient recipient = recipient();
		assertThatThrownBy(() -> NfeDocument.draft(NfeDocumentId.of(UUID.randomUUID()), COMPANY_ID, null,
				"Venda de mercadoria", recipient, List.of(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null,
				null, null, emptyTotals())).isInstanceOf(BusinessRuleException.class);
	}

	private NfeDocument draft(String naturezaOperacao, String referencedAccessKey) {
		return NfeDocument.draft(NfeDocumentId.of(UUID.randomUUID()), COMPANY_ID, null, naturezaOperacao, recipient(),
				List.of(item()), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, referencedAccessKey, null,
				emptyTotals());
	}

	private NfeRecipient recipient() {
		return new NfeRecipient(null, Document.cnpj("11222333000181"), "Cliente Exemplo LTDA", IeIndicator.TAXPAYER,
				"123456789", "SP");
	}

	private NfeItem item() {
		UUID productId = UUID.randomUUID();
		return new NfeItem(productId, BigDecimal.TEN, new BigDecimal("15.00"), BigDecimal.ZERO, "5102",
				new ItemTaxBreakdown(0, productId.toString(), List.of()));
	}

	private TaxCalculationTotals emptyTotals() {
		return new TaxCalculationTotals(new EnumMap<>(TaxType.class), BigDecimal.ZERO);
	}
}
