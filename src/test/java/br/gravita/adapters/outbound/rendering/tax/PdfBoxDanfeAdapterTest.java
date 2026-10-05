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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PdfBoxDanfeAdapterTest {

	private static final String VALID_CNPJ = "11.222.333/0001-81";

	@Test
	@DisplayName("Uses portrait as the default DANFE orientation")
	void ac4PortraitIsTheDefaultOrientation() throws Exception {
		final PdfBoxDanfeAdapter adapter = new PdfBoxDanfeAdapter(noLogoHttpClient());

		final byte[] pdf = adapter.generate(document(), company(null), DanfeOrientation.PORTRAIT);

		try (PDDocument loaded = Loader.loadPDF(pdf)) {
			assertThat(loaded.getNumberOfPages()).isEqualTo(1);
			final PDPage page = loaded.getPage(0);
			assertThat(page.getMediaBox().getWidth()).isEqualTo(PDRectangle.A4.getWidth());
			assertThat(page.getMediaBox().getHeight()).isEqualTo(PDRectangle.A4.getHeight());
		}
	}

	@Test
	@DisplayName("Renders the DANFE in landscape on request")
	void ac4LandscapeIsRenderedOnRequest() throws Exception {
		final PdfBoxDanfeAdapter adapter = new PdfBoxDanfeAdapter(noLogoHttpClient());

		final byte[] pdf = adapter.generate(document(), company(null), DanfeOrientation.LANDSCAPE);

		try (PDDocument loaded = Loader.loadPDF(pdf)) {
			final PDPage page = loaded.getPage(0);
			assertThat(page.getMediaBox().getWidth()).isEqualTo(PDRectangle.A4.getHeight());
			assertThat(page.getMediaBox().getHeight()).isEqualTo(PDRectangle.A4.getWidth());
		}
	}

	@Test
	@DisplayName("Embeds the access key barcode as an image on the page")
	void ac4TheAccessKeyBarcodeIsEmbeddedAsAnImageOnThePage() throws Exception {
		final PdfBoxDanfeAdapter adapter = new PdfBoxDanfeAdapter(noLogoHttpClient());

		final byte[] pdf = adapter.generate(document(), company(null), DanfeOrientation.PORTRAIT);

		try (PDDocument loaded = Loader.loadPDF(pdf)) {
			final List<COSName> objectNames = objectNames(loaded.getPage(0));
			assertThat(objectNames).isNotEmpty();
		}
	}

	@Test
	@DisplayName("Embeds the company logo when its URL is reachable")
	void ac4TheCompanyLogoIsEmbeddedWhenItsUrlIsReachable() throws Exception {
		final HttpClient httpClient = mock(HttpClient.class);
		@SuppressWarnings("unchecked")
		final HttpResponse<byte[]> response = mock(HttpResponse.class);
		when(response.statusCode()).thenReturn(200);
		when(response.body()).thenReturn(pngBytes());
		org.mockito.Mockito.doReturn(response).when(httpClient).send(any(HttpRequest.class), any());
		final PdfBoxDanfeAdapter adapter = new PdfBoxDanfeAdapter(httpClient);

		final byte[] withLogo = adapter.generate(document(), company("https://example.com/logo.png"), DanfeOrientation.PORTRAIT);
		final byte[] withoutLogo = adapter.generate(document(), company(null), DanfeOrientation.PORTRAIT);

		try (PDDocument loadedWithLogo = Loader.loadPDF(withLogo); PDDocument loadedWithoutLogo = Loader.loadPDF(withoutLogo)) {
			final int objectsWithLogo = objectNames(loadedWithLogo.getPage(0)).size();
			final int objectsWithoutLogo = objectNames(loadedWithoutLogo.getPage(0)).size();
			assertThat(objectsWithLogo).isGreaterThan(objectsWithoutLogo);
		}
	}

	@Test
	@DisplayName("Falls back to a text-only header when the logo URL is unreachable, instead of failing the DANFE")
	void ac4AnUnreachableLogoUrlFallsBackToATextOnlyHeaderInsteadOfFailingTheWholeDanfe() throws Exception {
		final HttpClient httpClient = mock(HttpClient.class);
		when(httpClient.send(any(HttpRequest.class), any())).thenThrow(new java.io.IOException("unreachable"));
		final PdfBoxDanfeAdapter adapter = new PdfBoxDanfeAdapter(httpClient);

		final byte[] pdf = adapter.generate(document(), company("https://example.com/logo.png"), DanfeOrientation.PORTRAIT);

		try (PDDocument loaded = Loader.loadPDF(pdf)) {
			assertThat(loaded.getNumberOfPages()).isEqualTo(1);
		}
	}

	private HttpClient noLogoHttpClient() {
		return mock(HttpClient.class);
	}

	private List<COSName> objectNames(final PDPage page) {
		return java.util.stream.StreamSupport.stream(page.getResources().getXObjectNames().spliterator(), false)
				.toList();
	}

	private byte[] pngBytes() throws Exception {
		final BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		ImageIO.write(image, "png", out);
		return out.toByteArray();
	}

	private Company company(final String logoUrl) {
		return Company.builder()
				.id(CompanyId.of(UUID.randomUUID()))
				.name("Acme Ltda")
				.cnpj(Document.cnpj(VALID_CNPJ))
				.ie("123456789")
				.im("987654")
				.cnae("6201500")
				.taxRegime(br.gravita.core.domain.masterdata.TaxRegime.SIMPLES_NACIONAL)
				.simplesOptante(true)
				.sefazEnvironment(SefazEnvironment.HOMOLOGATION)
				.address("Rua Teste, 100")
				.state("SP")
				.issuingEmail("nfe@example.com")
				.phone("11999999999")
				.logoUrl(logoUrl)
				.parentCompanyId(null)
				.build();
	}

	private NfeDocument document() {
		final UUID productId = UUID.randomUUID();
		final TaxLineBreakdown line = new TaxLineBreakdown(TaxType.ICMS, new BigDecimal("100.00"), new BigDecimal("18"),
				new BigDecimal("18.00"), new BigDecimal("18.00"), false, null);
		final ItemTaxBreakdown breakdown = new ItemTaxBreakdown(0, productId.toString(), List.of(line));
		final NfeItem item = new NfeItem(productId, "Produto Teste", BigDecimal.ONE, new BigDecimal("100.00"),
				BigDecimal.ZERO, breakdown);
		final NfeRecipient recipient = NfeRecipient.of(null, VALID_CNPJ, PersonType.COMPANY, "Cliente PJ Teste", "123456789",
				"RJ");
		final TaxCalculationTotals totals = TaxCalculationTotals.from(List.of(item.taxBreakdown()));

		return NfeDocument.builder()
				.id(NfeDocumentId.of(UUID.randomUUID()))
				.issuerCompanyId(CompanyId.of(UUID.randomUUID()))
				.originSalesOrderId(null)
				.naturezaOperacao(NaturezaOperacao.VENDA)
				.cfop(new Cfop("5102"))
				.recipient(recipient)
				.items(List.of(item))
				.freight(BigDecimal.ZERO)
				.insurance(BigDecimal.ZERO)
				.otherExpenses(BigDecimal.ZERO)
				.transport(null)
				.referencedAccessKey(null)
				.additionalInfo(null)
				.taxTotals(totals)
				.status(NfeDocumentStatus.SENT)
				.createdAt(Instant.now())
				.documentSeries("001")
				.documentNumber(42L)
				.accessKey("3".repeat(44))
				.sefazProtocol("PROTOCOL-1")
				.contingencyMode(false)
				.rejectionReason(null)
				.xmlStorageRef(null)
				.danfeStorageRef(null)
				.correctionLetters(List.of())
				.authorizedAt(null)
				.cancellationJustification(null)
				.cancelledAt(null)
				.build();
	}
}
