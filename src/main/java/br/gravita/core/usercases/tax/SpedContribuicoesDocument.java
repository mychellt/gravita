package br.gravita.core.usercases.tax;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * A fiscal document as the EFD Contribuições sees it (a {@code C100} and its {@code C170} items), whichever side it
 * came from: an NFe the company issued or one it received. The CST and the credit each item carries are decided once,
 * when the document is built, so the registers, the assessment and the summary all read the same figures.
 *
 * <p>{@code pis} and {@code cofins} are what the document itself states in total; the levies of its items are what the
 * company is assessed on.
 */
record SpedContribuicoesDocument(Operation operation, boolean issuedByCompany, Party party, String series,
		String number, String accessKey, LocalDate date, BigDecimal total, BigDecimal discount, BigDecimal goods,
		BigDecimal freight, BigDecimal insurance, BigDecimal otherExpenses, BigDecimal pis, BigDecimal cofins,
		List<Item> items) {

	/** {@code ENTRY} is a purchase (a credit, where the regime gives one), {@code EXIT} a sale (a contribution). */
	enum Operation {
		ENTRY,
		EXIT
	}

	/** The supplier of an entry or the recipient of an exit; {@code code} is what the {@code 0150} is keyed by. */
	record Party(String code, String name, String cnpj, String cpf) {
	}

	/** {@code value} is the item's gross value and {@code discount} what came off it. */
	record Item(String code, String description, String ncm, String unit, BigDecimal quantity, BigDecimal value,
			BigDecimal discount, String cfop, Levy pis, Levy cofins) {

		BigDecimal revenue() {
			return value.subtract(discount);
		}

		Levy levy(SpedTax tax) {
			return tax == SpedTax.PIS ? pis : cofins;
		}
	}

	/**
	 * What one contribution does on one item. {@code amount} is the contribution on an exit or the credit on an
	 * entry; it is {@code null}, with the base and the rate, when the item bears none (it is then CST
	 * {@code 49}/{@code 70}). {@code creditNature} is the {@code NAT_BC_CRED} of a credit and {@code null} otherwise.
	 */
	record Levy(String cst, BigDecimal base, BigDecimal rate, BigDecimal amount, String creditNature) {

		static Levy none(String cst) {
			return new Levy(cst, null, null, null, null);
		}

		boolean bearsAmount() {
			return amount != null;
		}
	}
}
