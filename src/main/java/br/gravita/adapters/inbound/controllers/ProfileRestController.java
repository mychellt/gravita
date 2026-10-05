package br.gravita.adapters.inbound.controllers;

import br.gravita.adapters.dtos.request.AssignProfilePermissionsRequest;
import br.gravita.adapters.dtos.response.ProfileResponse;
import br.gravita.adapters.dtos.response.ProfileSummaryResponse;
import br.gravita.adapters.inbound.controllers.security.AuthenticatedUser;
import br.gravita.core.domain.Context;
import br.gravita.core.domain.ProfileDomain;
import br.gravita.core.ports.business.AssignProfilePort;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.ports.business.FindProfilePort;
import br.gravita.core.usercases.system.ListProfilesUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/profiles")
public class ProfileRestController {

    private final AssignProfilePort assignProfilePort;
    private final FindProfilePort findProfilePort;
    private final ListProfilesUseCase listProfilesUseCase;

    @GetMapping
    public ResponseEntity<List<ProfileSummaryResponse>> findAll(@AuthenticatedUser UserId callerId) {
        return ResponseEntity.ok(listProfilesUseCase.execute().stream().map(ProfileSummaryResponse::from).toList());
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
