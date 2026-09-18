package br.gravita.adapters.inbound.controllers;

import br.gravita.adapters.dtos.request.AssignProfilePermissionsRequest;
import br.gravita.adapters.dtos.response.ProfileResponse;
import br.gravita.core.domain.Context;
import br.gravita.core.domain.ProfileDomain;
import br.gravita.core.ports.business.AssignProfilePort;
import br.gravita.core.ports.business.FindProfilePort;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/profiles")
public class ProfileRestController {

	private final AssignProfilePort assignProfilePort;
	private final FindProfilePort findProfilePort;

	public ProfileRestController(AssignProfilePort assignProfilePort, FindProfilePort findProfilePort) {
		this.assignProfilePort = assignProfilePort;
		this.findProfilePort = findProfilePort;
	}

	@PutMapping("/{id}/permissions")
	public ResponseEntity<ProfileResponse> assignPermissions(
			@PathVariable UUID id, @Valid @RequestBody AssignProfilePermissionsRequest request) {
		ProfileDomain updated = assignProfilePort.execute(new Context(request.toDomain(id)));
		return ResponseEntity.ok(ProfileResponse.from(updated));
	}

	@GetMapping("/{id}")
	public ResponseEntity<ProfileResponse> findById(@PathVariable UUID id) {
		return ResponseEntity.ok(ProfileResponse.from(findProfilePort.execute(new Context(id))));
	}
}
