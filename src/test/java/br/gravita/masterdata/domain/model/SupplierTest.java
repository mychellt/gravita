package br.gravita.masterdata.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.gravita.core.domain.masterdata.*;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.domain.shared.PersonType;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SupplierTest {

	private static final Document VALID_CNPJ = Document.cnpj("11222333000181");
	private static final Document VALID_CPF = Document.cpf("52998224725");
	private static final Address VALID_ADDRESS =
			new Address("Rua Teste", "100", null, "Centro", "Sao Paulo", "SP", "01000-000");

	@Test
	@DisplayName("Registers a PJ supplier with the optional purchasing fields absent")
	void shouldRegisterPjSupplierWithOptionalPurchasingFieldsAbsent() {
		final Supplier supplier = validBuilder().build();

		assertThat(supplier.personType()).isEqualTo(PersonType.COMPANY);
		assertThat(supplier.getDocument()).isEqualTo(VALID_CNPJ);
		assertThat(supplier.getBankAccount()).isNull();
		assertThat(supplier.getPixKey()).isNull();
		assertThat(supplier.getAverageLeadTimeDays()).isNull();
		assertThat(supplier.getDefaultPurchaseCfop()).isNull();
	}

	@Test
	@DisplayName("Registers a PF supplier reusing the shared document value object")
	void shouldRegisterPfSupplierReusingSharedDocumentValueObject() {
		final Supplier supplier = validBuilder().document(VALID_CPF).build();

		assertThat(supplier.personType()).isEqualTo(PersonType.INDIVIDUAL);
		assertThat(supplier.getDocument()).isEqualTo(VALID_CPF);
	}

	@Test
	@DisplayName("Rejects a null document")
	void shouldRejectNullDocument() {
		assertThatThrownBy(() -> build(b -> b.document(null)))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("document");
	}

	@Test
	@DisplayName("Rejects a blank name")
	void shouldRejectBlankName() {
		assertThatThrownBy(() -> build(b -> b.name(" ")))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("name");
	}

	@Test
	@DisplayName("Rejects an empty address list")
	void shouldRejectEmptyAddressList() {
		assertThatThrownBy(() -> build(b -> b.addresses(List.of())))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("address");
	}

	@Test
	@DisplayName("Allows an empty contacts list")
	void shouldAllowEmptyContactsList() {
		assertThatCode(() -> build(b -> b.contacts(List.of()))).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("Accepts an optional bank account and PIX key")
	void shouldAcceptOptionalBankAccountAndPixKey() {
		final BankAccount bankAccount = new BankAccount("001", "1234", "56789-0");
		final PixKey pixKey = PixKey.of("supplier@example.com");

		final Supplier supplier = validBuilder().bankAccount(bankAccount).pixKey(pixKey).build();

		assertThat(supplier.getBankAccount()).isEqualTo(bankAccount);
		assertThat(supplier.getPixKey()).isEqualTo(pixKey);
	}

	@Test
	@DisplayName("Rejects a non-positive average lead time in days")
	void shouldRejectNonPositiveAverageLeadTimeDays() {
		assertThatThrownBy(() -> build(b -> b.averageLeadTimeDays(0)))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("lead time");
	}

	@Test
	@DisplayName("Rejects a malformed default purchase CFOP")
	void shouldRejectMalformedDefaultPurchaseCfop() {
		assertThatThrownBy(() -> build(b -> b.defaultPurchaseCfop("abc")))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("CFOP");
	}

	@Test
	@DisplayName("Allows using the supplier in a purchase order once both purchasing fields are set")
	void shouldAllowUsingSupplierInPurchaseOrderOnceBothPurchasingFieldsAreSet() {
		final Supplier supplier = validBuilder().averageLeadTimeDays(5).defaultPurchaseCfop("1102").build();

		assertThatCode(supplier::assertReadyForPurchasing).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("Rejects purchase order use when the average lead time is missing")
	void shouldRejectPurchaseOrderUseWhenAverageLeadTimeDaysIsMissing() {
		final Supplier supplier = validBuilder().defaultPurchaseCfop("1102").build();

		assertThatThrownBy(supplier::assertReadyForPurchasing)
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("lead time");
	}

	@Test
	@DisplayName("Rejects purchase order use when the default purchase CFOP is missing")
	void shouldRejectPurchaseOrderUseWhenDefaultPurchaseCfopIsMissing() {
		final Supplier supplier = validBuilder().averageLeadTimeDays(5).build();

		assertThatThrownBy(supplier::assertReadyForPurchasing)
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("CFOP");
	}

	private void build(final java.util.function.Consumer<Builder> customize) {
		final Builder builder = validBuilder();
		customize.accept(builder);
		builder.build();
	}

	private Builder validBuilder() {
		return new Builder();
	}

	private static final class Builder {
		private SupplierId id = SupplierId.of(UUID.randomUUID());
		private Document document = VALID_CNPJ;
		private String name = "Acme Supplies";
		private List<Address> addresses = List.of(VALID_ADDRESS);
		private List<Contact> contacts = List.of(new Contact(ContactType.EMAIL, "contact@acme.com"));
		private BankAccount bankAccount = null;
		private PixKey pixKey = null;
		private Integer averageLeadTimeDays = null;
		private String defaultPurchaseCfop = null;

		Builder document(final Document document) {
			this.document = document;
			return this;
		}

		Builder name(final String name) {
			this.name = name;
			return this;
		}

		Builder addresses(final List<Address> addresses) {
			this.addresses = addresses;
			return this;
		}

		Builder contacts(final List<Contact> contacts) {
			this.contacts = contacts;
			return this;
		}

		Builder bankAccount(final BankAccount bankAccount) {
			this.bankAccount = bankAccount;
			return this;
		}

		Builder pixKey(final PixKey pixKey) {
			this.pixKey = pixKey;
			return this;
		}

		Builder averageLeadTimeDays(final Integer averageLeadTimeDays) {
			this.averageLeadTimeDays = averageLeadTimeDays;
			return this;
		}

		Builder defaultPurchaseCfop(final String defaultPurchaseCfop) {
			this.defaultPurchaseCfop = defaultPurchaseCfop;
			return this;
		}

		Supplier build() {
			return Supplier.of(id, document, name, addresses, contacts, bankAccount, pixKey, averageLeadTimeDays,
					defaultPurchaseCfop);
		}
	}
}
