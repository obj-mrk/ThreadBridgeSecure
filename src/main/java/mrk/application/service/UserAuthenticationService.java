package mrk.application.service;

import mrk.application.exception.AuthenticationException;
import mrk.application.port.TransactionManager;
import mrk.application.port.UserRepository;

public class UserAuthenticationService {
    private final TransactionManager transactionManager;
    private final UserRepository userRepository;
    private final ApiTokenService apiTokenService;

    public UserAuthenticationService(
            TransactionManager transactionManager,
            UserRepository userRepository,
            ApiTokenService apiTokenService
    ) {
        this.transactionManager = transactionManager;
        this.userRepository = userRepository;
        this.apiTokenService = apiTokenService;
    }

    public long authenticate(String apiToken) {
        if (apiToken == null || apiToken.isBlank()) {
            throw new AuthenticationException("Invalid authentication credentials");
        }

        String tokenHash = apiTokenService.hash(apiToken);

        return transactionManager.execute(connection ->
                userRepository
                        .findActiveUserIdByApiTokenHash(connection, tokenHash)
                        .orElseThrow(() ->
                                new AuthenticationException(
                                        "Invalid authentication credentials"
                                )
                        )
        );
    }
}