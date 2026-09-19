package br.gravita.system.adapter.in.web;

import br.gravita.shared.BusinessRuleException;
import br.gravita.system.application.port.in.RegisterUserUseCase;
import br.gravita.system.application.port.in.UpdateUserUseCase;
import br.gravita.system.domain.model.UserId;
import br.gravita.system.domain.model.UserNotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

	private final RegisterUserUseCase registerUserUseCase;
	private final UpdateUserUseCase updateUserUseCase;

	public UserController(RegisterUserUseCase registerUserUseCase, UpdateUserUseCase updateUserUseCase) {
		this.registerUserUseCase = registerUserUseCase;
		this.updateUserUseCase = updateUserUseCase;
	}

	@PostMapping
	public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterUserRequest request) {
		UserId id = registerUserUseCase.execute(request.toCommand());
		return ResponseEntity.created(URI.create("/api/users/" + id.value())).body(UserResponse.from(id));
	}

	@PatchMapping("/{id}")
	public ResponseEntity<Void> update(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request) {
		updateUserUseCase.execute(request.toCommand(id));
		return ResponseEntity.noContent().build();
	}

	@ExceptionHandler(BusinessRuleException.class)
	public ResponseEntity<Map<String, String>> handleBusinessRuleException(BusinessRuleException exception) {
		return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
	}

	@ExceptionHandler(UserNotFoundException.class)
	public ResponseEntity<Map<String, String>> handleUserNotFoundException(UserNotFoundException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", exception.getMessage()));
	}
}
