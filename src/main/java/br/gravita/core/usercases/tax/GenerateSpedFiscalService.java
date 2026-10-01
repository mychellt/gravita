package br.gravita.core.usercases.tax;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.masterdata.Company;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.FiscalDocumentType;
import br.gravita.core.domain.tax.InboundNfe;
import br.gravita.core.domain.tax.NfeDocument;
import br.gravita.core.domain.tax.VoidedNumberRange;
import br.gravita.core.ports.inbound.tax.GenerateSpedFiscalCommand;
import br.gravita.core.ports.inbound.tax.GenerateSpedFiscalCommand.Period;
import br.gravita.core.ports.inbound.tax.GenerateSpedFiscalUseCase;
import br.gravita.core.ports.inbound.tax.SpedFiscalFile;
import br.gravita.core.ports.inbound.tax.SpedValidationException;
import br.gravita.core.ports.inbound.tax.SpedValidationReport;
import br.gravita.core.ports.outbound.persistence.CompanyRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.InboundNfeRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.NfeRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.VoidedNumberRangeRepositoryPort;
import br.gravita.core.ports.outbound.tax.GenerateSpedFilePort;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Builds the EFD ICMS/IPI file of a company's period (UC-M2-11) from what {@code tax} already holds - the NFe the
 * company issued that SEFAZ authorized and that are authorized or cancelled, the NFe it received and confirmed,
 * and the number ranges it voided - and has the text laid out. Nothing is written but the file.
 *
 * <p>The mandatory records are populated and checked <em>before</em> any text is produced: when one cannot be, the
 * request fails with a report of every record missing or invalid instead of an incomplete file. A document belongs
 * to the period of the day SEFAZ authorized it (received NFe: the day their supplier issued them) in the clock's
 * time zone.
 */
@UseCase
public class GenerateSpedFiscalService implements GenerateSpedFiscalUseCase {

	private final CompanyRepositoryPort companyRepositoryPort;
	private final NfeRepositoryPort nfeRepositoryPort;
	private final InboundNfeRepositoryPort inboundNfeRepositoryPort;
	private final VoidedNumberRangeRepositoryPort voidedNumberRangeRepositoryPort;
	private final GenerateSpedFilePort generateSpedFilePort;
	private final Clock clock;

	@Autowired
	public GenerateSpedFiscalService(CompanyRepositoryPort companyRepositoryPort, NfeRepositoryPort nfeRepositoryPort,
			InboundNfeRepositoryPort inboundNfeRepositoryPort,
			VoidedNumberRangeRepositoryPort voidedNumberRangeRepositoryPort,
			GenerateSpedFilePort generateSpedFilePort) {
		this(companyRepositoryPort, nfeRepositoryPort, inboundNfeRepositoryPort, voidedNumberRangeRepositoryPort,
				generateSpedFilePort, Clock.systemDefaultZone());
	}

	public GenerateSpedFiscalService(CompanyRepositoryPort companyRepositoryPort, NfeRepositoryPort nfeRepositoryPort,
			InboundNfeRepositoryPort inboundNfeRepositoryPort,
			VoidedNumberRangeRepositoryPort voidedNumberRangeRepositoryPort,
			GenerateSpedFilePort generateSpedFilePort, Clock clock) {
		this.companyRepositoryPort = companyRepositoryPort;
		this.nfeRepositoryPort = nfeRepositoryPort;
		this.inboundNfeRepositoryPort = inboundNfeRepositoryPort;
		this.voidedNumberRangeRepositoryPort = voidedNumberRangeRepositoryPort;
		this.generateSpedFilePort = generateSpedFilePort;
		this.clock = clock;
	}

	@Override
	public SpedFiscalFile execute(GenerateSpedFiscalCommand command) {
		CompanyId companyId = command.companyId();
		Company company = companyRepositoryPort.findById(companyId)
				.orElseThrow(() -> new ResourceNotFoundException("Company not found: " + companyId.value()));

		Period period = command.period();
		ZoneId zone = clock.getZone();
		Instant from = period.start().atStartOfDay(zone).toInstant();
		Instant to = period.end().plusDays(1).atStartOfDay(zone).toInstant();

		List<NfeDocument> issued = nfeRepositoryPort.findAuthorizedOrCancelledByCompanyBetween(companyId, from, to);
		List<InboundNfe> received = inboundNfeRepositoryPort.findConfirmedByCompanyBetween(companyId, from, to);
		List<VoidedNumberRange> voided = voidedNumberRangeRepositoryPort
				.findByCompanyIdAndVoidedAtBetween(companyId, from, to).stream()
				.filter(range -> range.getDocumentType() == FiscalDocumentType.NFE).toList();

		SpedFiscalRecords records = new SpedFiscalRecords(command, company, zone);
		records.addIssued(issued);
		records.addReceived(received);
		records.addVoided(voided);

		SpedValidationReport report = records.report();
		if (report.hasErrors()) {
			throw new SpedValidationException(report);
		}
		return new SpedFiscalFile(fileName(company, period), generateSpedFilePort.generate(records.blocks()), report);
	}

	private static String fileName(Company company, Period period) {
		return "SPED-EFD-ICMS-IPI-" + company.getCnpj().number() + "-" + YearMonth.from(period.start()) + ".txt";
	}
}
