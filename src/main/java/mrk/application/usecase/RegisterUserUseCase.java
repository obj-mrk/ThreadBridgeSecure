package mrk.application.usecase;

import mrk.application.command.RegisterUserCommand;
import mrk.application.exception.ConflictException;
import mrk.application.exception.ValidationException;
import mrk.application.port.TransactionManager;
import mrk.application.port.UserRepository;
import mrk.application.result.RegisterUserResult;
import mrk.domain.model.User;
import mrk.domain.value.UserStatus;

import java.time.LocalDateTime;

public class RegisterUserUseCase {
    private final TransactionManager transactionManager;
    private final UserRepository userRepository;

    public RegisterUserUseCase(
            TransactionManager transactionManager,
            UserRepository userRepository
    ) {
        this.transactionManager = transactionManager;
        this.userRepository = userRepository;
    }

    public RegisterUserResult register(RegisterUserCommand command) {
        validate(command);

        return transactionManager.execute(connection -> {
            if (userRepository.existsByUsername(connection, command.getUsername())) {
                throw new ConflictException("Username already exists");
            }

            User user = new User();
            user.setUsername(command.getUsername().trim());
            user.setStatus(UserStatus.ACTIVE);
            user.setCreatedAt(LocalDateTime.now());

            long userId = userRepository.save(connection, user);

            return new RegisterUserResult(
                    userId,
                    user.getUsername(),
                    user.getStatus().name()
            );
        });
    }

    private void validate(RegisterUserCommand command) {
        if (command == null) {
            throw new ValidationException("Command must not be null");
        }

        if (command.getUsername() == null || command.getUsername().isBlank()) {
            throw new ValidationException("Username must not be blank");
        }

        if (command.getUsername().length() > 100) {
            throw new ValidationException("Username must not be longer than 100 characters");
        }
    }
}