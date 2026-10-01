package br.gravita.adapters.outbound.rendering.tax;

import br.gravita.core.ports.inbound.tax.LivrosFiscaisBooks;
import br.gravita.core.ports.outbound.reporting.RenderPdfPort;
import br.gravita.core.ports.outbound.reporting.RenderPdfPort.PdfReport;
import br.gravita.core.ports.outbound.tax.GenerateFiscalBookPort;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;

/**
 * Renders the books of a period (UC-M2-13). It lays them out once as a {@link PdfReport} ({@link FiscalBookLayout})
 * and prints that same report both ways, so the PDF and the TXT carry the same cells and totals. The PDF goes
 * through the shared {@link RenderPdfPort} - the one renderer M9's fiscal books and report exports also use - so
 * there is no second PDF renderer for book content; only the TXT is written here, as plain fixed-width text.
 */
@Component
class FiscalBookAdapter implements GenerateFiscalBookPort {

	private final RenderPdfPort renderPdfPort;

	FiscalBookAdapter(RenderPdfPort renderPdfPort) {
		this.renderPdfPort = renderPdfPort;
	}

	@Override
	public FiscalBookFiles generate(LivrosFiscaisBooks books) {
		PdfReport report = FiscalBookLayout.layout(books);
		return new FiscalBookFiles(renderPdfPort.render(report),
				FiscalBookLayout.toText(report).getBytes(StandardCharsets.UTF_8));
	}
}
