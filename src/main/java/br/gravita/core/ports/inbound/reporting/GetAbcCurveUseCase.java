package br.gravita.core.ports.inbound.reporting;

import java.util.List;

public interface GetAbcCurveUseCase {
	List<AbcCurveEntry> execute(AbcCurveQuery query);
}
