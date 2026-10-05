package br.gravita.core.usercases.system;

import br.gravita.core.domain.system.ProfileReference;

import java.util.List;

public interface ListProfilesUseCase {
	List<ProfileReference> execute();
}
