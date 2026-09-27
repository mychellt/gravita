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
	public ImportSupplierNfeXmlService(InboundNfeRepositoryPort inboundNfeRepositoryPort,
			XmlObjectStoragePort xmlObjectStoragePort) {
		this(inboundNfeRepositoryPort, xmlObjectStoragePort, new NfeXmlParser());
	}

	ImportSupplierNfeXmlService(InboundNfeRepositoryPort inboundNfeRepositoryPort,
			XmlObjectStoragePort xmlObjectStoragePort, NfeXmlParser xmlParser) {
		this.inboundNfeRepositoryPort = inboundNfeRepositoryPort;
		this.xmlObjectStoragePort = xmlObjectStoragePort;
		this.xmlParser = xmlParser;
	}

	@Override
	public InboundNfe execute(ImportSupplierNfeXmlCommand command) {
		ParsedSupplierNfe parsed = xmlParser.parse(command.xmlFile());

		String xmlStorageRef = xmlObjectStoragePort.store(command.companyId(), command.xmlFile());

		InboundNfe inboundNfe = InboundNfe.importedFromXml(
				InboundNfeId.of(UUID.randomUUID()),
				command.companyId(),
				parsed.accessKey(),
				parsed.series(),
				parsed.number(),
				Document.cnpj(parsed.supplierCnpj()),
				parsed.supplierName(),
				parsed.issuedAt(),
				parsed.items(),
				parsed.totals(),
				xmlStorageRef);

		return inboundNfeRepositoryPort.save(inboundNfe);
	}
}
