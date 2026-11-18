package mrk.http.handler;

import com.sun.net.httpserver.HttpExchange;
import mrk.application.exception.AuthenticationException;
import mrk.application.exception.ValidationException;
import mrk.application.result.SecureInboxItem;
import mrk.application.service.UserAuthenticationService;
import mrk.application.usecase.GetSecureInboxUseCase;
import mrk.http.response.ErrorResponse;
import mrk.http.response.SecureInboxItemResponse;
import mrk.utils.JsonUtils;

import java.io.IOException;
import java.util.List;

public class SecureInboxHandler extends BaseHttpHandler {
    private final JsonUtils jsonUtils;
    private final GetSecureInboxUseCase getSecureInboxUseCase;
    private final UserAuthenticationService userAuthenticationService;

    public SecureInboxHandler(
            JsonUtils jsonUtils,
            GetSecureInboxUseCase getSecureInboxUseCase, UserAuthenticationService userAuthenticationService
    ) {
        this.jsonUtils = jsonUtils;
        this.getSecureInboxUseCase = getSecureInboxUseCase;
        this.userAuthenticationService = userAuthenticationService;
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

            long recipientId = userAuthenticationService.authenticate(extractBearerToken(exchange));

            List<SecureInboxItemResponse> response = getSecureInboxUseCase.getInbox(recipientId)
                    .stream()
                    .map(this::toResponse)
                    .toList();

            sendJson(exchange, 200, jsonUtils.toJson(response));
        } catch (AuthenticationException exception) {
            sendJson(exchange, 401, jsonUtils.toJson(new ErrorResponse("ERROR", "Invalid authentication credentials")));
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
}