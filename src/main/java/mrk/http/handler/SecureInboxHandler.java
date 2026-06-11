package mrk.http.handler;

import com.sun.net.httpserver.HttpExchange;
import mrk.application.exception.ValidationException;
import mrk.application.result.SecureInboxItem;
import mrk.application.usecase.GetSecureInboxUseCase;
import mrk.http.response.ErrorResponse;
import mrk.http.response.SecureInboxItemResponse;
import mrk.utils.JsonUtils;

import java.io.IOException;
import java.util.List;

public class SecureInboxHandler extends BaseHttpHandler {
    private static final String USER_ID_HEADER = "X-User-Id";

    private final JsonUtils jsonUtils;
    private final GetSecureInboxUseCase getSecureInboxUseCase;

    public SecureInboxHandler(
            JsonUtils jsonUtils,
            GetSecureInboxUseCase getSecureInboxUseCase
    ) {
        this.jsonUtils = jsonUtils;
        this.getSecureInboxUseCase = getSecureInboxUseCase;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if (!isMethod(exchange, "GET")) {
                sendMethodNotAllowed(exchange);
                return;
            }

            String path = exchange.getRequestURI().getPath();
            if (!"/me/secure-messages".equals(path)) {
                sendJson(exchange, 404, jsonUtils.toJson(new ErrorResponse("ERROR", "Endpoint not found")));
                return;
            }

            long recipientId = extractUserId(exchange);

            List<SecureInboxItemResponse> response = getSecureInboxUseCase.getInbox(recipientId)
                    .stream()
                    .map(this::toResponse)
                    .toList();

            sendJson(exchange, 200, jsonUtils.toJson(response));
        } catch (ValidationException | IllegalArgumentException exception) {
            sendJson(exchange, 400, jsonUtils.toJson(new ErrorResponse("ERROR", exception.getMessage())));
        } catch (Exception exception) {
            sendJson(exchange, 500, jsonUtils.toJson(new ErrorResponse("ERROR", "Internal Server Error")));
        }
    }

    private SecureInboxItemResponse toResponse(SecureInboxItem item) {
        return new SecureInboxItemResponse(
                item.getMessageId(),
                item.getSenderId(),
                item.getStatus().name(),
                item.isOneTime(),
                item.getCreatedAt(),
                item.getExpiresAt()
        );
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