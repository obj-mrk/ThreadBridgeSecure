package mrk.http.handler;

import com.sun.net.httpserver.HttpExchange;
import mrk.application.command.RegisterUserCommand;
import mrk.application.exception.ConflictException;
import mrk.application.exception.ValidationException;
import mrk.application.result.RegisterUserResult;
import mrk.application.usecase.RegisterUserUseCase;
import mrk.http.request.RegisterUserRequest;
import mrk.http.response.ErrorResponse;
import mrk.http.response.RegisterUserResponse;
import mrk.utils.JsonUtils;

import java.io.IOException;

public class UserHandler extends BaseHttpHandler {
    private final JsonUtils jsonUtils;
    private final RegisterUserUseCase registerUserUseCase;

    public UserHandler(JsonUtils jsonUtils, RegisterUserUseCase registerUserUseCase) {
        this.jsonUtils = jsonUtils;
        this.registerUserUseCase = registerUserUseCase;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if (!isMethod(exchange, "POST")) {
                sendMethodNotAllowed(exchange);
                return;
            }

            String path = exchange.getRequestURI().getPath();
            if (!"/users".equals(path) && !"/users/".equals(path)) {
                sendJson(exchange, 404, jsonUtils.toJson(new ErrorResponse("ERROR", "Endpoint not found")));
                return;
            }

            String body = readBody(exchange);
            RegisterUserRequest request = jsonUtils.fromJson(body, RegisterUserRequest.class);

            RegisterUserCommand command = new RegisterUserCommand(request.getUsername());
            RegisterUserResult result = registerUserUseCase.register(command);

            RegisterUserResponse response = new RegisterUserResponse(
                    "CREATED",
                    result.getUserId(),
                    result.getUsername()
            );

            sendJson(exchange, 201, jsonUtils.toJson(response));
        } catch (ValidationException exception) {
            sendJson(exchange, 400, jsonUtils.toJson(new ErrorResponse("ERROR", exception.getMessage())));
        } catch (ConflictException exception) {
            sendJson(exchange, 409, jsonUtils.toJson(new ErrorResponse("ERROR", exception.getMessage())));
        } catch (IllegalArgumentException exception) {
            sendJson(exchange, 400, jsonUtils.toJson(new ErrorResponse("ERROR", exception.getMessage())));
        } catch (Exception exception) {
            sendJson(exchange, 500, jsonUtils.toJson(new ErrorResponse("ERROR", "Internal Server Error")));
        }
    }
}