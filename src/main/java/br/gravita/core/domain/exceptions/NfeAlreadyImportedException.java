package br.gravita.core.domain.exceptions;

/** An inbound NF-e with the same access key was already imported. */
public class NfeAlreadyImportedException extends DuplicateResourceException {
	public NfeAlreadyImportedException(final String accessKey) {
		super("An NFe with access key " + accessKey + " has already been imported");
	}
}
