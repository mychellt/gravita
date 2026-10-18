package br.gravita.core.usercases.tax;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.exceptions.NfeAlreadyImportedException;
import br.gravita.core.domain.masterdata.Company;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.domain.tax.InboundNfe;
import br.gravita.core.domain.tax.InboundNfeId;
import br.gravita.core.ports.inbound.tax.ImportSupplierNfeXmlCommand;
import br.gravita.core.ports.inbound.tax.ImportSupplierNfeXmlUseCase;
import br.gravita.core.ports.outbound.persistence.CompanyRepositoryPort;
import br.gravita.core.ports.outbound.persistence.XmlObjectStoragePort;
import br.gravita.core.ports.outbound.persistence.tax.InboundNfeRepositoryPort;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

@UseCase
public class ImportSupplierNfeXmlService implements ImportSupplierNfeXmlUseCase {

	private final InboundNfeRepositoryPort inboundNfeRepositoryPort;
	private final XmlObjectStoragePort xmlObjectStoragePort;
	private final CompanyRepositoryPort companyRepositoryPort;
	private final NfeXmlParser xmlParser;

	@Autowired
	public ImportSupplierNfeXmlService(final InboundNfeRepositoryPort inboundNfeRepositoryPort,
			final XmlObjectStoragePort xmlObjectStoragePort, final CompanyRepositoryPort companyRepositoryPort) {
		this(inboundNfeRepositoryPort, xmlObjectStoragePort, companyRepositoryPort, new NfeXmlParser());
	}

	ImportSupplierNfeXmlService(final InboundNfeRepositoryPort inboundNfeRepositoryPort,
			final XmlObjectStoragePort xmlObjectStoragePort, final CompanyRepositoryPort companyRepositoryPort,
			final NfeXmlParser xmlParser) {
		this.inboundNfeRepositoryPort = inboundNfeRepositoryPort;
		this.companyRepositoryPort = companyRepositoryPort;
		this.xmlObjectStoragePort = xmlObjectStoragePort;
		this.xmlParser = xmlParser;
	}

	@Override
	public InboundNfe execute(final ImportSupplierNfeXmlCommand command) {
		final ParsedSupplierNfe parsed = xmlParser.parse(command.xmlFile());
		requireAddressedToCompany(command, parsed);
		inboundNfeRepositoryPort.findByAccessKey(parsed.accessKey()).ifPresent(existing -> {
			throw new NfeAlreadyImportedException(parsed.accessKey());
		});

		final String xmlStorageRef = xmlObjectStoragePort.store(command.companyId(), command.xmlFile());

		final InboundNfe inboundNfe = InboundNfe.importedFromXml()
				.id(InboundNfeId.of(UUID.randomUUID()))
				.companyId(command.companyId())
				.accessKey(parsed.accessKey())
				.series(parsed.series())
				.number(parsed.number())
				.supplierDocument(Document.cnpj(parsed.supplierCnpj()))
				.supplierName(parsed.supplierName())
				.issuedAt(parsed.issuedAt())
				.items(parsed.items())
				.totals(parsed.totals())
				.xmlStorageRef(xmlStorageRef)
				.build();

		return inboundNfeRepositoryPort.save(inboundNfe);
	}

	/** The note must be addressed to the logged-in company: importing someone else's NF-e is never allowed. */
	private void requireAddressedToCompany(final ImportSupplierNfeXmlCommand command, final ParsedSupplierNfe parsed) {
		final Company company = companyRepositoryPort.findById(command.companyId())
				.orElseThrow(() -> new BusinessRuleException("Company not found: " + command.companyId().value()));
		final String companyDocument = company.getCnpj().number();
		if (parsed.recipientDocument() == null || !parsed.recipientDocument().equals(companyDocument)) {
			throw new BusinessRuleException("This NF-e is not addressed to your company (recipient CNPJ differs from "
					+ companyDocument + "); only notes issued to the logged-in company can be imported");
		}
	}
}
