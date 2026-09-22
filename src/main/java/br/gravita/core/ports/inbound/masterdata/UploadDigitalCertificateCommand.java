package br.gravita.core.ports.inbound.masterdata;

import br.gravita.core.domain.masterdata.CompanyId;

public record UploadDigitalCertificateCommand(CompanyId companyId, String certificateType, byte[] pfxFile,
		String password) {
}
