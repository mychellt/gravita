package br.gravita.core.ports.outbound.persistence.tax;

import br.gravita.core.domain.tax.MunicipalityIntegration;
import java.util.Optional;

public interface MunicipalityIntegrationRepositoryPort {

	MunicipalityIntegration save(MunicipalityIntegration integration);

	Optional<MunicipalityIntegration> findByIbgeCode(String ibgeCode);
}
