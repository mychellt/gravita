package br.gravita.core.ports.outbound.tax;

import br.gravita.core.domain.tax.SefazUnavailableException;

public interface SubmitToSefazPort {

	SefazSubmissionResult submit(SefazSubmissionRequest request);

	SefazSubmissionResult cancel(SefazCancellationRequest request);

	SefazSubmissionResult voidNumberRange(SefazVoidNumberRangeRequest request);
}
