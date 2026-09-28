package br.gravita.finance;

import java.nio.charset.StandardCharsets;

/** Builds EMV BR Code payloads with a correct CRC16 for tests (independent of the domain's validator). */
public final class PixPayloads {

	private PixPayloads() {
	}

	public static String valid() {
		return withCrc("00020126360014br.gov.bcb.pix0114+55119999999995204000053039865802BR5909GRAVITA6009SAO PAULO");
	}

	public static String withCrc(String body) {
		String content = body + "6304";
		return content + String.format("%04X", crc16(content));
	}

	public static int crc16(String content) {
		int crc = 0xFFFF;
		for (byte b : content.getBytes(StandardCharsets.UTF_8)) {
			for (int bit = 7; bit >= 0; bit--) {
				boolean bitSet = ((b >> bit) & 1) == 1;
				boolean topBit = (crc & 0x8000) != 0;
				crc <<= 1;
				if (bitSet ^ topBit) {
					crc ^= 0x1021;
				}
			}
		}
		return crc & 0xFFFF;
	}
}
