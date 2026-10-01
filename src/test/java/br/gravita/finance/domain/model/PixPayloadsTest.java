package br.gravita.finance.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import br.gravita.finance.PixPayloads;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PixPayloadsTest {

	@Test
	@DisplayName("Computes the CRC16 with the CCITT-FALSE variant")
	void crc16UsesTheCcittFalseVariant() {
		// standard CRC-16/CCITT-FALSE check value
		assertThat(PixPayloads.crc16("123456789")).isEqualTo(0x29B1);
	}
}
