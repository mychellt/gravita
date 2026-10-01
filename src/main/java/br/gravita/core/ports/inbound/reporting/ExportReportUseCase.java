package br.gravita.core.ports.inbound.reporting;

public interface ExportReportUseCase {
	ExportedFile execute(ExportReportQuery query);
}
