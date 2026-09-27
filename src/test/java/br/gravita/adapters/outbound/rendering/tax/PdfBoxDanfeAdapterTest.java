package br.gravita.adapters.outbound.rendering.tax;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.masterdata.Company;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.SefazEnvironment;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.domain.shared.PersonType;
import br.gravita.core.domain.tax.Cfop;
import br.gravita.core.domain.tax.ItemTaxBreakdown;
import br.gravita.core.domain.tax.NaturezaOperacao;
import br.gravita.core.domain.tax.NfeDocument;
import br.gravita.core.domain.tax.NfeDocumentId;
import br.gravita.core.domain.tax.NfeDocumentStatus;
import br.gravita.core.domain.tax.NfeItem;
import br.gravita.core.domain.tax.NfeRecipient;
import br.gravita.core.domain.tax.TaxCalculationTotals;
import br.gravita.core.domain.tax.TaxLineBreakdown;
import br.gravita.core.domain.tax.TaxType;
import br.gravita.core.ports.outbound.tax.DanfeOrientation;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;

class PdfBoxDanfeAdapterTest {

	private static final String VALID_CNPJ = "11.222.333/0001-81";

	@Test
	void ac4_portraitIsTheDefaultOrientation() throws Exception {
		PdfBoxDanfeAdapter adapter = new PdfBoxDanfeAdapter(noLogoHttpClient());

		byte[] pdf = adapter.generate(document(), company(null), DanfeOrientation.PORTRAIT);

		try (PDDocument loaded = Loader.loadPDF(pdf)) {
			assertThat(loaded.getNumberOfPages()).isEqualTo(1);
			PDPage page = loaded.getPage(0);
			assertThat(page.getMediaBox().getWidth()).isEqualTo(PDRectangle.A4.getWidth());
			assertThat(page.getMediaBox().getHeight()).isEqualTo(PDRectangle.A4.getHeight());
		}
	}

	@Test
	void ac4_landscapeIsRenderedOnRequest() throws Exception {
		PdfBoxDanfeAdapter adapter = new PdfBoxDanfeAdapter(noLogoHttpClient());

		byte[] pdf = adapter.generate(document(), company(null), DanfeOrientation.LANDSCAPE);

		try (PDDocument loaded = Loader.loadPDF(pdf)) {
			PDPage page = loaded.getPage(0);
			assertThat(page.getMediaBox().getWidth()).isEqualTo(PDRectangle.A4.getHeight());
			assertThat(page.getMediaBox().getHeight()).isEqualTo(PDRectangle.A4.getWidth());
		}
	}

	@Test
	void ac4_theAccessKeyBarcodeIsEmbeddedAsAnImageOnThePage() throws Exception {
		PdfBoxDanfeAdapter adapter = new PdfBoxDanfeAdapter(noLogoHttpClient());

		byte[] pdf = adapter.generate(document(), company(null), DanfeOrientation.PORTRAIT);

		try (PDDocument loaded = Loader.loadPDF(pdf)) {
			List<COSName> xObjectNames = xObjectNames(loaded.getPage(0));
			assertThat(xObjectNames).isNotEmpty();
		}
	}

	@Test
	void ac4_theCompanyLogoIsEmbeddedWhenItsUrlIsReachable() throws Exception {
		HttpClient httpClient = mock(HttpClient.class);
		@SuppressWarnings("unchecked")
		HttpResponse<byte[]> response = mock(HttpResponse.class);
		when(response.statusCode()).thenReturn(200);
		when(response.body()).thenReturn(pngBytes());
		org.mockito.Mockito.doReturn(response).when(httpClient).send(any(HttpRequest.class), any());
		PdfBoxDanfeAdapter adapter = new PdfBoxDanfeAdapter(httpClient);

		byte[] withLogo = adapter.generate(document(), company("https://example.com/logo.png"), DanfeOrientation.PORTRAIT);
		byte[] withoutLogo = adapter.generate(document(), company(null), DanfeOrientation.PORTRAIT);

		try (PDDocument loadedWithLogo = Loader.loadPDF(withLogo); PDDocument loadedWithoutLogo = Loader.loadPDF(withoutLogo)) {
			int xObjectsWithLogo = xObjectNames(loadedWithLogo.getPage(0)).size();
			int xObjectsWithoutLogo = xObjectNames(loadedWithoutLogo.getPage(0)).size();
			assertThat(xObjectsWithLogo).isGreaterThan(xObjectsWithoutLogo);
		}
	}

	@Test
	void ac4_anUnreachableLogoUrlFallsBackToATextOnlyHeaderInsteadOfFailingTheWholeDanfe() throws Exception {
		HttpClient httpClient = mock(HttpClient.class);
		when(httpClient.send(any(HttpRequest.class), any())).thenThrow(new java.io.IOException("unreachable"));
		PdfBoxDanfeAdapter adapter = new PdfBoxDanfeAdapter(httpClient);

		byte[] pdf = adapter.generate(document(), company("https://example.com/logo.png"), DanfeOrientation.PORTRAIT);

		try (PDDocument loaded = Loader.loadPDF(pdf)) {
			assertThat(loaded.getNumberOfPages()).isEqualTo(1);
		}
	}

	private HttpClient noLogoHttpClient() {
		return mock(HttpClient.class);
	}

	private List<COSName> xObjectNames(PDPage page) {
		return java.util.stream.StreamSupport.stream(page.getResources().getXObjectNames().spliterator(), false)
				.toList();
	}

	private byte[] pngBytes() throws Exception {
		BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ImageIO.write(image, "png", out);
		return out.toByteArray();
	}

	private Company company(String logoUrl) {
		return Company.of(CompanyId.of(UUID.randomUUID()), Document.cnpj(VALID_CNPJ), "123456789", "987654",
				"6201500", br.gravita.core.domain.masterdata.TaxRegime.SIMPLES_NACIONAL, true,
				SefazEnvironment.HOMOLOGATION, "Rua Teste, 100", "SP", "nfe@example.com", "11999999999", logoUrl,
				null);
	}

	private NfeDocument document() {
		UUID productId = UUID.randomUUID();
		TaxLineBreakdown line = new TaxLineBreakdown(TaxType.ICMS, new BigDecimal("100.00"), new BigDecimal("18"),
				new BigDecimal("18.00"), new BigDecimal("18.00"), false, null);
		ItemTaxBreakdown breakdown = new ItemTaxBreakdown(0, productId.toString(), List.of(line));
		NfeItem item = new NfeItem(productId, "Produto Teste", BigDecimal.ONE, new BigDecimal("100.00"),
				BigDecimal.ZERO, breakdown);
		NfeRecipient recipient = NfeRecipient.of(null, VALID_CNPJ, PersonType.COMPANY, "Cliente PJ Teste", "123456789",
				"RJ");
		TaxCalculationTotals totals = TaxCalculationTotals.from(List.of(item.taxBreakdown()));

		return NfeDocument.of(NfeDocumentId.of(UUID.randomUUID()), CompanyId.of(UUID.randomUUID()), null,
				NaturezaOperacao.VENDA, new Cfop("5102"), recipient, List.of(item), BigDecimal.ZERO, BigDecimal.ZERO,
				BigDecimal.ZERO, null, null, null, totals, NfeDocumentStatus.SENT, Instant.now(), "001", 42L,
				"3".repeat(44), "PROTOCOL-1", false, null, null, null, List.of(), null, null, null);
	}
}
