package br.gravita.core.usercases.tax;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.domain.tax.InboundNfe;
import br.gravita.core.domain.tax.InboundNfeId;
import br.gravita.core.ports.inbound.tax.ImportSupplierNfeXmlCommand;
import br.gravita.core.ports.inbound.tax.ImportSupplierNfeXmlUseCase;
import br.gravita.core.ports.outbound.persistence.XmlObjectStoragePort;
import br.gravita.core.ports.outbound.persistence.tax.InboundNfeRepositoryPort;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

@UseCase
public class ImportSupplierNfeXmlService implements ImportSupplierNfeXmlUseCase {

	private final InboundNfeRepositoryPort inboundNfeRepositoryPort;
	private final XmlObjectStoragePort xmlObjectStoragePort;
	private final NfeXmlParser xmlParser;

	@Autowired
	public ImportSupplierNfeXmlService(final InboundNfeRepositoryPort inboundNfeRepositoryPort,
			final XmlObjectStoragePort xmlObjectStoragePort) {
		this(inboundNfeRepositoryPort, xmlObjectStoragePort, new NfeXmlParser());
	}

	ImportSupplierNfeXmlService(final InboundNfeRepositoryPort inboundNfeRepositoryPort,
			final XmlObjectStoragePort xmlObjectStoragePort, final NfeXmlParser xmlParser) {
		this.inboundNfeRepositoryPort = inboundNfeRepositoryPort;
		this.xmlObjectStoragePort = xmlObjectStoragePort;
		this.xmlParser = xmlParser;
	}

	@Override
	public InboundNfe execute(final ImportSupplierNfeXmlCommand command) {
		final ParsedSupplierNfe parsed = xmlParser.parse(command.xmlFile());

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
}
