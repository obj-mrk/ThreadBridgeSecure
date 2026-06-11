package mrk.http.handler;

import com.sun.net.httpserver.HttpExchange;
import mrk.application.command.DecryptSecureMessageCommand;
import mrk.application.exception.NotFoundException;
import mrk.application.exception.ValidationException;
import mrk.application.result.DecryptedMessageResult;
import mrk.application.usecase.DecryptSecureMessageUseCase;
import mrk.http.response.DecryptedMessageResponse;
import mrk.http.response.ErrorResponse;
import mrk.utils.JsonUtils;

import java.io.IOException;

public class DecryptSecureMessageHandler extends BaseHttpHandler {
    private static final String USER_ID_HEADER = "X-User-Id";

    private final JsonUtils jsonUtils;
    private final DecryptSecureMessageUseCase decryptSecureMessageUseCase;

    public DecryptSecureMessageHandler(
            JsonUtils jsonUtils,
            DecryptSecureMessageUseCase decryptSecureMessageUseCase
    ) {
        this.jsonUtils = jsonUtils;
        this.decryptSecureMessageUseCase = decryptSecureMessageUseCase;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if (!isMethod(exchange, "POST")) {
                sendMethodNotAllowed(exchange);
                return;
            }

            long messageId = extractMessageId(exchange.getRequestURI().getPath());
            long recipientId = extractUserId(exchange);

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

    private long extractUserId(HttpExchange exchange) {
        String value = exchange.getRequestHeaders().getFirst(USER_ID_HEADER);

        if (value == null || value.isBlank()) {
            throw new ValidationException("X-User-Id header is required");
        }

        try {
            long userId = Long.parseLong(value.trim());

            if (userId <= 0) {
                throw new ValidationException("X-User-Id must be positive");
            }

            return userId;
        } catch (NumberFormatException exception) {
            throw new ValidationException("X-User-Id must be a number");
        }
    }
}