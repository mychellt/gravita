package br.gravita.core.usercases.tax;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.tax.NfseDocument;
import br.gravita.core.domain.tax.NfseId;
import br.gravita.core.domain.tax.NfseNumber;
import br.gravita.core.domain.tax.RpsId;
import br.gravita.core.ports.inbound.tax.ConvertRpsToNfseCommand;
import br.gravita.core.ports.inbound.tax.ConvertRpsToNfseUseCase;
import br.gravita.core.ports.outbound.persistence.tax.NfseRepositoryPort;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-M4-03. Converts issued RPS into {@code DRAFT} NFSe documents, individually or in batch, assigning each the next
 * number of its {@code (company, municipality)} sequence. Nothing is transmitted - that is UC-M4-04.
 *
 * <p>The whole command is one transaction: a missing RPS fails the batch and no number is consumed. Converting an RPS
 * that is already converted is idempotent - it yields the same id and allocates nothing.
 */
@UseCase
public class ConvertRpsToNfseService implements ConvertRpsToNfseUseCase {

	private final NfseRepositoryPort nfseRepositoryPort;

	public ConvertRpsToNfseService(final NfseRepositoryPort nfseRepositoryPort) {
		this.nfseRepositoryPort = nfseRepositoryPort;
	}

	@Override
	@Transactional
	public List<NfseId> execute(final ConvertRpsToNfseCommand command) {
		final Set<NfseId> distinct = new LinkedHashSet<>();
		for (final RpsId rpsId : command.rpsIds()) {
			distinct.add(rpsId.toNfseId());
		}

		// A stable lock order keeps two overlapping batches from deadlocking on each other's rows.
		final List<NfseId> lockOrder = new ArrayList<>(distinct);
		lockOrder.sort(Comparator.comparing(NfseId::value));
		for (final NfseId id : lockOrder) {
			convert(id);
		}

		// AC1: the same ids come back in the order they were asked for.
		return List.copyOf(distinct);
	}

	private void convert(final NfseId id) {
		final NfseDocument document = nfseRepositoryPort.findByIdForUpdate(id)
				.orElseThrow(() -> new ResourceNotFoundException("RPS not found: " + id.value()));
		if (!document.isRps()) {
			// AC4: already converted (or further along) - nothing to do, no second document and no number burned.
			return;
		}
		// AC2: numbered within (company, municipality) of the provider, apart from the NFe/RPS series.
		final NfseNumber number = nfseRepositoryPort.allocateNextNumber(document.getProviderCompanyId(),
				document.getProviderMunicipalityIbgeCode());
		// AC3: DRAFT with a timestamp; the transmission itself is a separate use case.
		nfseRepositoryPort.save(document.convertToNfse(number.series(), number.number(), Instant.now()));
	}
}
