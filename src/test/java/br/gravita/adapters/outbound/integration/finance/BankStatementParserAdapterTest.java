package br.gravita.adapters.outbound.integration.finance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.gravita.core.domain.finance.BankStatementLine;
import br.gravita.core.domain.shared.BusinessRuleException;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class BankStatementParserAdapterTest {

	private final BankStatementParserAdapter parser = new BankStatementParserAdapter();

	private static final String OFX_SGML = """
			OFXHEADER:100
			DATA:OFXSGML
			VERSION:102

			<OFX>
			<BANKMSGSRSV1><STMTTRNRS><STMTRS>
			<BANKTRANLIST>
			<DTSTART>20260901
			<STMTTRN>
			<TRNTYPE>CREDIT
			<DTPOSTED>20260925120000[-3:BRT]
			<TRNAMT>1234.56
			<FITID>A1
			<MEMO>PIX RECEBIDO
			</STMTTRN>
			<STMTTRN>
			<TRNTYPE>DEBIT
			<DTPOSTED>20260926
			<TRNAMT>-80.00
			<FITID>A2
			<NAME>FORNECEDOR &amp; CIA
			</STMTTRN>
			</BANKTRANLIST>
			</STMTRS></STMTTRNRS></BANKMSGSRSV1>
			</OFX>
			""";

	@Test
	void parsesAnSgmlOfxStatement() {
		List<BankStatementLine> lines = parser.parse(OFX_SGML);

		assertThat(lines).hasSize(2);
		BankStatementLine credit = lines.get(0);
		assertThat(credit.getLineNumber()).isEqualTo(1);
		assertThat(credit.getPostedOn()).isEqualTo(LocalDate.of(2026, 9, 25));
		assertThat(credit.getAmount()).isEqualByComparingTo("1234.56");
		assertThat(credit.getDescription()).isEqualTo("PIX RECEBIDO");
		assertThat(credit.getReference()).isEqualTo("A1");
		BankStatementLine debit = lines.get(1);
		assertThat(debit.getLineNumber()).isEqualTo(2);
		assertThat(debit.getPostedOn()).isEqualTo(LocalDate.of(2026, 9, 26));
		assertThat(debit.getAmount()).isEqualByComparingTo("-80.00");
		assertThat(debit.getDescription()).isEqualTo("FORNECEDOR & CIA");
		assertThat(lines).noneMatch(BankStatementLine::isMatched);
	}

	@Test
	void parsesAnXmlOfxStatement() {
		String xml = """
				<?xml version="1.0"?>
				<OFX><BANKMSGSRSV1><STMTTRNRS><STMTRS><BANKTRANLIST>
				<STMTTRN><TRNTYPE>CREDIT</TRNTYPE><DTPOSTED>20260925</DTPOSTED><TRNAMT>10,50</TRNAMT><FITID>X</FITID><MEMO>Deposito</MEMO></STMTTRN>
				</BANKTRANLIST></STMTRS></STMTTRNRS></BANKMSGSRSV1></OFX>
				""";

		List<BankStatementLine> lines = parser.parse(xml);

		assertThat(lines).singleElement().satisfies(line -> {
			assertThat(line.getPostedOn()).isEqualTo(LocalDate.of(2026, 9, 25));
			assertThat(line.getAmount()).isEqualByComparingTo("10.50");
			assertThat(line.getDescription()).isEqualTo("Deposito");
			assertThat(line.getReference()).isEqualTo("X");
		});
	}

	@Test
	void anOfxWithoutTransactionsHasNoLines() {
		assertThat(parser.parse("<OFX><BANKTRANLIST></BANKTRANLIST></OFX>")).isEmpty();
	}

	@Test
	void anOfxTransactionNeedsADateAndAnAmount() {
		assertThatThrownBy(() -> parser.parse("<OFX><STMTTRN><TRNAMT>1.00</TRNAMT></STMTTRN></OFX>"))
				.isInstanceOf(BusinessRuleException.class).hasMessageContaining("OFX transaction 1");
		assertThatThrownBy(() -> parser.parse("<OFX><STMTTRN><DTPOSTED>20260925</DTPOSTED></STMTTRN></OFX>"))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void anOfxTransactionWithAnInvalidDateOrAmountIsRejected() {
		assertThatThrownBy(() -> parser
				.parse("<OFX><STMTTRN><DTPOSTED>20261399</DTPOSTED><TRNAMT>1.00</TRNAMT></STMTTRN></OFX>"))
				.isInstanceOf(BusinessRuleException.class).hasMessageContaining("DTPOSTED");
		assertThatThrownBy(() -> parser
				.parse("<OFX><STMTTRN><DTPOSTED>20260925</DTPOSTED><TRNAMT>abc</TRNAMT></STMTTRN></OFX>"))
				.isInstanceOf(BusinessRuleException.class).hasMessageContaining("amount");
	}

	@Test
	void parsesACommaSeparatedCsv() {
		String csv = """
				date,amount,description,reference
				2026-09-25,100.00,PIX RECEBIDO,R1
				2026-09-26,-80.5,"Pagamento, fornecedor \"\"ACME\"\"",R2
				""";

		List<BankStatementLine> lines = parser.parse(csv);

		assertThat(lines).hasSize(2);
		assertThat(lines.get(0).getLineNumber()).isEqualTo(2);
		assertThat(lines.get(0).getPostedOn()).isEqualTo(LocalDate.of(2026, 9, 25));
		assertThat(lines.get(0).getAmount()).isEqualByComparingTo("100.00");
		assertThat(lines.get(0).getReference()).isEqualTo("R1");
		assertThat(lines.get(1).getLineNumber()).isEqualTo(3);
		assertThat(lines.get(1).getAmount()).isEqualByComparingTo("-80.5");
		assertThat(lines.get(1).getDescription()).isEqualTo("Pagamento, fornecedor \"ACME\"");
	}

	@Test
	void parsesABrazilianSemicolonCsv() {
		String csv = "﻿Data;Valor;Histórico;Documento\r\n"
				+ "25/09/2026;\"1.234,56\";TED RECEBIDA;123\r\n"
				+ "\r\n"
				+ "26/09/2026;R$ -80,00;TARIFA;\r\n";

		List<BankStatementLine> lines = parser.parse(csv);

		assertThat(lines).hasSize(2);
		assertThat(lines.get(0).getPostedOn()).isEqualTo(LocalDate.of(2026, 9, 25));
		assertThat(lines.get(0).getAmount()).isEqualByComparingTo("1234.56");
		assertThat(lines.get(0).getDescription()).isEqualTo("TED RECEBIDA");
		assertThat(lines.get(0).getReference()).isEqualTo("123");
		assertThat(lines.get(1).getLineNumber()).isEqualTo(4);
		assertThat(lines.get(1).getPostedOn()).isEqualTo(LocalDate.of(2026, 9, 26));
		assertThat(lines.get(1).getAmount()).isEqualByComparingTo("-80.00");
		assertThat(lines.get(1).getReference()).isNull();
	}

	@Test
	void theOptionalCsvColumnsMayBeAbsent() {
		List<BankStatementLine> lines = parser.parse("date;amount\n2026-09-25;10");

		assertThat(lines).singleElement().satisfies(line -> {
			assertThat(line.getDescription()).isEmpty();
			assertThat(line.getReference()).isNull();
		});
	}

	@Test
	void aCsvNeedsADateAndAnAmountColumn() {
		assertThatThrownBy(() -> parser.parse("date,description\n2026-09-25,x"))
				.isInstanceOf(BusinessRuleException.class).hasMessageContaining("date and an amount");
		assertThatThrownBy(() -> parser.parse("amount,description\n1.00,x"))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void aCsvRowWithAMissingOrInvalidValueIsRejectedWithItsFileLine() {
		assertThatThrownBy(() -> parser.parse("date,amount\n2026-09-25,1.00\n2026-09-26,"))
				.isInstanceOf(BusinessRuleException.class).hasMessageContaining("Line 3");
		assertThatThrownBy(() -> parser.parse("date,amount\n25-09-2026,1.00"))
				.isInstanceOf(BusinessRuleException.class).hasMessageContaining("invalid date");
		assertThatThrownBy(() -> parser.parse("date,amount\n2026-09-25,ten"))
				.isInstanceOf(BusinessRuleException.class).hasMessageContaining("invalid amount");
	}

	@Test
	void aCsvWithOnlyAHeaderHasNoLines() {
		assertThat(parser.parse("date,amount\n")).isEmpty();
	}

	@Test
	void aBlankStatementIsRejected() {
		assertThatThrownBy(() -> parser.parse("  ")).isInstanceOf(BusinessRuleException.class);
		assertThatThrownBy(() -> parser.parse(null)).isInstanceOf(BusinessRuleException.class);
	}
}
