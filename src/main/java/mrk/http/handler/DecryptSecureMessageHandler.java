package mrk.http.handler;

import com.sun.net.httpserver.HttpExchange;
import mrk.application.command.DecryptSecureMessageCommand;
import mrk.application.exception.AuthenticationException;
import mrk.application.exception.NotFoundException;
import mrk.application.exception.ValidationException;
import mrk.application.result.DecryptedMessageResult;
import mrk.application.service.UserAuthenticationService;
import mrk.application.usecase.DecryptSecureMessageUseCase;
import mrk.http.response.DecryptedMessageResponse;
import mrk.http.response.ErrorResponse;
import mrk.utils.JsonUtils;

import java.io.IOException;

public class DecryptSecureMessageHandler extends BaseHttpHandler {
    private final JsonUtils jsonUtils;
    private final DecryptSecureMessageUseCase decryptSecureMessageUseCase;
    private final UserAuthenticationService userAuthenticationService;

    public DecryptSecureMessageHandler(
            JsonUtils jsonUtils,
            DecryptSecureMessageUseCase decryptSecureMessageUseCase, UserAuthenticationService userAuthenticationService
    ) {
        this.jsonUtils = jsonUtils;
        this.decryptSecureMessageUseCase = decryptSecureMessageUseCase;
        this.userAuthenticationService = userAuthenticationService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if (!isMethod(exchange, "POST")) {
                sendMethodNotAllowed(exchange);
                return;
            }

            long messageId = extractMessageId(exchange.getRequestURI().getPath());
            long recipientId = userAuthenticationService.authenticate(extractBearerToken(exchange));

            DecryptSecureMessageCommand command = new DecryptSecureMessageCommand(
                    messageId,
                    recipientId
            );

            DecryptedMessageResult result = decryptSecureMessageUseCase.decrypt(command);

            DecryptedMessageResponse response = new DecryptedMessageResponse(
                    "DECRYPTED",
                    result.getMessageId(),
                    result.getSenderId(),
                    result.getText(),
                    result.isDestroyedAfterRead()
            );

            sendJson(exchange, 200, jsonUtils.toJson(response));
        } catch (AuthenticationException exception) {
            sendJson(exchange, 401, jsonUtils.toJson(new ErrorResponse("ERROR", "Invalid authentication credentials")));
        } catch (NotFoundException exception) {
            sendJson(exchange, 404, jsonUtils.toJson(new ErrorResponse("ERROR", exception.getMessage())));
        } catch (ValidationException exception) {
            sendJson(exchange, 409, jsonUtils.toJson(new ErrorResponse("ERROR", exception.getMessage())));
        } catch (IllegalArgumentException exception) {
            sendJson(exchange, 400, jsonUtils.toJson(new ErrorResponse("ERROR", exception.getMessage())));
        } catch (Exception exception) {
            sendJson(exchange, 500, jsonUtils.toJson(new ErrorResponse("ERROR", "Internal Server Error")));
        }
    }

    private long extractMessageId(String path) {
        String[] parts = path.split("/");

        if (parts.length != 4
                || !"secure-messages".equals(parts[1])
                || !"decrypt".equals(parts[3])) {
            throw new ValidationException("Expected path: /secure-messages/{id}/decrypt");
        }

        try {
            long messageId = Long.parseLong(parts[2]);

            if (messageId <= 0) {
                throw new ValidationException("Message id must be positive");
            }

            return messageId;
        } catch (NumberFormatException exception) {
            throw new ValidationException("Message id must be a number");
        }
    }
}