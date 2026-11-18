package mrk.application.usecase;

import mrk.application.command.RegisterUserCommand;
import mrk.application.exception.ConflictException;
import mrk.application.exception.ValidationException;
import mrk.application.port.TransactionManager;
import mrk.application.port.UserRepository;
import mrk.application.result.RegisterUserResult;
import mrk.application.service.ApiTokenService;
import mrk.domain.model.User;

import java.time.LocalDateTime;

public class RegisterUserUseCase {

    private final TransactionManager transactionManager;
    private final UserRepository userRepository;
    private final ApiTokenService apiTokenService;

    public RegisterUserUseCase(
            TransactionManager transactionManager,
            UserRepository userRepository,
            ApiTokenService apiTokenService
    ) {
        this.transactionManager = transactionManager;
        this.userRepository = userRepository;
        this.apiTokenService = apiTokenService;
    }

    public RegisterUserResult register(RegisterUserCommand command) {
        validate(command);

        String apiToken = apiTokenService.generateToken();
        String apiTokenHash = apiTokenService.hash(apiToken);

        return transactionManager.execute(connection -> {
            if (userRepository.existsByUsername(connection, command.getUsername())) {
                throw new ConflictException("Username already exists");
            }

            User user = User.register(
                    command.getUsername(),
                    LocalDateTime.now()
            );

            long userId = userRepository.save(
                    connection,
                    user,
                    apiTokenHash
            );

            return new RegisterUserResult(
                    userId,
                    user.getUsername(),
                    user.getStatus().name(),
                    apiToken
            );
        });
    }

    private void validate(RegisterUserCommand command) {
        if (command == null) {
            throw new ValidationException("Command must not be null");
        }
    }
}