package mrk.http.handler;

import com.sun.net.httpserver.HttpExchange;
import mrk.application.command.RegisterUserKeyCommand;
import mrk.application.exception.AuthenticationException;
import mrk.application.exception.ConflictException;
import mrk.application.exception.NotFoundException;
import mrk.application.exception.ValidationException;
import mrk.application.result.RegisterUserKeyResult;
import mrk.application.service.UserAuthenticationService;
import mrk.application.usecase.RegisterUserKeyUseCase;
import mrk.http.request.RegisterUserKeyRequest;
import mrk.http.response.ErrorResponse;
import mrk.http.response.RegisterUserKeyResponse;
import mrk.utils.JsonUtils;

import java.io.IOException;

public class UserKeyHandler extends BaseHttpHandler {
    private final JsonUtils jsonUtils;
    private final RegisterUserKeyUseCase registerUserKeyUseCase;
    private final UserAuthenticationService userAuthenticationService;

    public UserKeyHandler(JsonUtils jsonUtils, RegisterUserKeyUseCase registerUserKeyUseCase, UserAuthenticationService userAuthenticationService) {
        this.jsonUtils = jsonUtils;
        this.registerUserKeyUseCase = registerUserKeyUseCase;
        this.userAuthenticationService = userAuthenticationService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if (!isMethod(exchange, "POST")) {
                sendMethodNotAllowed(exchange);
                return;
            }

            String path = exchange.getRequestURI().getPath();
            long userId = extractUserId(path);

            String body = readBody(exchange);
            RegisterUserKeyRequest request = jsonUtils.fromJson(body, RegisterUserKeyRequest.class);

            RegisterUserKeyCommand command = new RegisterUserKeyCommand(
                    userId,
                    request.getPublicKeyPem(),
                    request.getPrivateKeyPem(),
                    request.getAlgorithm()
            );

            RegisterUserKeyResult result = registerUserKeyUseCase.register(command);

            RegisterUserKeyResponse response = new RegisterUserKeyResponse(
                    "ACTIVE",
                    result.getKeyId(),
                    result.getUserId(),
                    result.getKeyFingerprint(),
                    result.getAlgorithm()
            );

            sendJson(exchange, 201, jsonUtils.toJson(response));
        } catch (AuthenticationException exception) {
            sendJson(exchange, 401, jsonUtils.toJson(new ErrorResponse("ERROR", "Invalid authentication credentials")));
        } catch (ValidationException exception) {
            sendJson(exchange, 400, jsonUtils.toJson(new ErrorResponse("ERROR", exception.getMessage())));
        } catch (NotFoundException exception) {
            sendJson(exchange, 404, jsonUtils.toJson(new ErrorResponse("ERROR", exception.getMessage())));
        } catch (ConflictException exception) {
            sendJson(exchange, 409, jsonUtils.toJson(new ErrorResponse("ERROR", exception.getMessage())));
        } catch (IllegalArgumentException exception) {
            sendJson(exchange, 400, jsonUtils.toJson(new ErrorResponse("ERROR", exception.getMessage())));
        } catch (Exception exception) {
            sendJson(exchange, 500, jsonUtils.toJson(new ErrorResponse("ERROR", "Internal Server Error")));
        }
    }

    private long extractUserId(String path) {
        String[] parts = path.split("/");

        if (parts.length != 4 || !"users".equals(parts[1]) || !"keys".equals(parts[3])) {
            throw new ValidationException("Expected path: /users/{id}/keys");
        }

        try {
            return Long.parseLong(parts[2]);
        } catch (NumberFormatException exception) {
            throw new ValidationException("User id must be a number");
        }
    }
}