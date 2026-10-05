package br.gravita.adapters.outbound.rendering.tax;

import br.gravita.core.domain.masterdata.Company;
import br.gravita.core.domain.tax.NfeDocument;
import br.gravita.core.domain.tax.NfeItem;
import br.gravita.core.ports.outbound.tax.DanfeOrientation;
import br.gravita.core.ports.outbound.tax.GenerateDanfePort;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import javax.imageio.ImageIO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Component;

/**
 * Renders a DANFE PDF with PDFBox (UC-M2-03, AC4) - no PDF library existed
 * anywhere in the codebase before this ticket. This is a functional, single-
 * page layout carrying the fields the AC calls out (logo, barcode, item
 * list, totals) rather than a pixel-perfect reproduction of SEFAZ's official
 * DANFE template, which is a much larger, separately-scoped effort.
 * Fetching the company's {@code logoUrl} is best-effort: any failure (no
 * URL, unreachable, unsupported format) falls back to a text-only header
 * instead of failing the whole DANFE.
 */
@Component
public class PdfBoxDanfeAdapter implements GenerateDanfePort {

	private static final float MARGIN = 40f;
	private static final Duration LOGO_FETCH_TIMEOUT = Duration.ofSeconds(2);

	private final HttpClient httpClient;

	public PdfBoxDanfeAdapter() {
		this.httpClient = HttpClient.newBuilder().connectTimeout(LOGO_FETCH_TIMEOUT).build();
	}

	PdfBoxDanfeAdapter(final HttpClient httpClient) {
		this.httpClient = httpClient;
	}

	@Override
	public byte[] generate(final NfeDocument document, final Company company, final DanfeOrientation orientation) {
		try (PDDocument pdf = new PDDocument()) {
			final PDRectangle pageSize = orientation == DanfeOrientation.LANDSCAPE
					? new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth())
					: PDRectangle.A4;
			final PDPage page = new PDPage(pageSize);
			pdf.addPage(page);

			final PDFont font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
			final PDFont bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

			try (PDPageContentStream content = new PDPageContentStream(pdf, page)) {
				final float width = pageSize.getWidth();
				float y = pageSize.getHeight() - MARGIN;

				y = drawHeader(pdf, content, company, bold, font, width, y);
				y = drawText(content, bold, 12, MARGIN, y, "DANFE - Documento Auxiliar da Nota Fiscal Eletronica");
				y = drawText(content, font, 10, MARGIN, y,
						"NFe " + safe(document.getDocumentSeries()) + "/" + document.getDocumentNumber());
				y = drawText(content, font, 10, MARGIN, y, "Chave de acesso: " + safe(document.getAccessKey()));
				y = drawText(content, font, 10, MARGIN, y, "Protocolo: " + safe(document.getSefazProtocol()));
				y -= 10;

				y = drawText(content, bold, 10, MARGIN, y, "Destinatario");
				y = drawText(content, font, 10, MARGIN, y, safe(document.getRecipient().name()) + " - "
						+ safe(document.getRecipient().document().number()));
				y -= 10;

				y = drawText(content, bold, 10, MARGIN, y, "Itens");
				for (final NfeItem item : document.getItems()) {
					y = drawText(content, font, 9, MARGIN, y,
							safe(item.description()) + "  qtd " + item.quantity() + "  unit " + item.unitPrice()
									+ "  total " + item.lineTotal());
					if (y < MARGIN + 120) {
						break; // single-page layout: further lines are truncated rather than paginated.
					}
				}
				y -= 10;
				y = drawText(content, bold, 11, MARGIN, y, "Valor total: " + document.getDocumentTotal());

				drawBarcode(pdf, content, document.getAccessKey(), width, MARGIN);
			}

			final ByteArrayOutputStream out = new ByteArrayOutputStream();
			pdf.save(out);
			return out.toByteArray();
		} catch (final IOException e) {
			throw new IllegalStateException("Failed to render DANFE for NfeDocument " + document.getId().value(), e);
		}
	}

	private float drawHeader(final PDDocument pdf, final PDPageContentStream content, final Company company, final PDFont bold, final PDFont font,
			final float pageWidth, final float y) throws IOException {
		final PDImageXObject logo = fetchLogo(pdf, company.getLogoUrl());
		if (logo != null) {
			final float logoHeight = 40f;
			final float logoWidth = logoHeight * logo.getWidth() / logo.getHeight();
			content.drawImage(logo, MARGIN, y - logoHeight, logoWidth, logoHeight);
		}
		return drawText(content, bold, 12, MARGIN, y, "CNPJ: " + company.getCnpj().number());
	}

	private PDImageXObject fetchLogo(final PDDocument pdf, final String logoUrl) {
		if (logoUrl == null || logoUrl.isBlank()) {
			return null;
		}
		try {
			final HttpRequest request = HttpRequest.newBuilder(URI.create(logoUrl)).timeout(LOGO_FETCH_TIMEOUT).GET().build();
			final HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
			if (response.statusCode() != 200) {
				return null;
			}
			final BufferedImage image = ImageIO.read(new java.io.ByteArrayInputStream(response.body()));
			if (image == null) {
				return null;
			}
			return LosslessFactory.createFromImage(pdf, image);
		} catch (final Exception e) {
			// Best-effort: a broken/unreachable logo must not block DANFE generation.
			return null;
		}
	}

	private void drawBarcode(final PDDocument pdf, final PDPageContentStream content, final String accessKey, final float pageWidth,
			final float margin) throws IOException {
		if (accessKey == null || accessKey.isBlank()) {
			return;
		}
		final BitMatrix matrix = new Code128Writer().encode(accessKey, BarcodeFormat.CODE_128, 400, 60);
		final BufferedImage barcodeImage = MatrixToImageWriter.toBufferedImage(matrix);
		final PDImageXObject barcode = LosslessFactory.createFromImage(pdf, barcodeImage);
		final float barcodeWidth = pageWidth - (2 * margin);
		content.drawImage(barcode, margin, margin, barcodeWidth, 40f);
	}

	private float drawText(final PDPageContentStream content, final PDFont font, final float fontSize, final float x, final float y, final String text)
			throws IOException {
		content.beginText();
		content.setFont(font, fontSize);
		content.newLineAtOffset(x, y);
		content.showText(text == null ? "" : text);
		content.endText();
		return y - (fontSize + 6);
	}

	private String safe(final String value) {
		return value == null ? "" : value;
	}
}
