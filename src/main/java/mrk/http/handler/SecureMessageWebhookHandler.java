package mrk.http.handler;

import com.sun.net.httpserver.HttpExchange;
import mrk.application.command.AcceptSecureMessageCommand;
import mrk.application.exception.AuthenticationException;
import mrk.application.exception.NotFoundException;
import mrk.application.exception.ValidationException;
import mrk.application.result.AcceptSecureMessageResult;
import mrk.application.service.UserAuthenticationService;
import mrk.application.usecase.AcceptSecureMessageUseCase;
import mrk.http.request.AcceptSecureMessageRequest;
import mrk.http.response.AcceptedMessageResponse;
import mrk.http.response.ErrorResponse;
import mrk.utils.JsonUtils;

import java.io.IOException;

public class SecureMessageWebhookHandler extends BaseHttpHandler {
    private final JsonUtils jsonUtils;
    private final AcceptSecureMessageUseCase acceptSecureMessageUseCase;
    private final UserAuthenticationService userAuthenticationService;

    public SecureMessageWebhookHandler(
            JsonUtils jsonUtils,
            AcceptSecureMessageUseCase acceptSecureMessageUseCase,
            UserAuthenticationService userAuthenticationService
    ) {
        this.jsonUtils = jsonUtils;
        this.acceptSecureMessageUseCase = acceptSecureMessageUseCase;
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

            if (!"/webhook/secure-messages".equals(path)) {
                sendJson(
                        exchange,
                        404,
                        jsonUtils.toJson(
                                new ErrorResponse(
                                        "ERROR",
                                        "Endpoint not found"
                                )
                        )
                );
                return;
            }

            long senderId = userAuthenticationService.authenticate(
                    extractBearerToken(exchange)
            );

            String body = readBody(exchange);

            AcceptSecureMessageRequest request = jsonUtils.fromJson(
                    body,
                    AcceptSecureMessageRequest.class
            );

            validateHttpRequest(request);

            AcceptSecureMessageCommand command = new AcceptSecureMessageCommand(
                    request.getRequestId(),
                    senderId,
                    request.getRecipientId(),
                    request.getText(),
                    request.getTtlSeconds(),
                    request.getOneTime()
            );

            AcceptSecureMessageResult result =
                    acceptSecureMessageUseCase.accept(command);

            AcceptedMessageResponse response =
                    new AcceptedMessageResponse(
                            "ACCEPTED",
                            result.getInboundMessageId(),
                            result.getRequestId(),
                            result.isDuplicate()
                    );

            sendJson(exchange, 202, jsonUtils.toJson(response));

        } catch (AuthenticationException exception) {
            sendJson(
                    exchange,
                    401,
                    jsonUtils.toJson(
                            new ErrorResponse(
                                    "ERROR",
                                    "Invalid authentication credentials"
                            )
                    )
            );

        } catch (ValidationException exception) {
            sendJson(exchange, 400, jsonUtils.toJson(new ErrorResponse("ERROR", exception.getMessage())));

        } catch (NotFoundException exception) {
            sendJson(exchange, 404, jsonUtils.toJson(new ErrorResponse("ERROR", exception.getMessage())));

        } catch (IllegalArgumentException exception) {
            sendJson(exchange, 400, jsonUtils.toJson(new ErrorResponse("ERROR", exception.getMessage())));

        } catch (Exception exception) {
            sendJson(exchange, 500, jsonUtils.toJson(new ErrorResponse("ERROR", "Internal Server Error")));
        }
    }

    private void validateHttpRequest(AcceptSecureMessageRequest request) {
        if (request == null) {
            throw new ValidationException(
                    "Request body must not be empty"
            );
        }

        if (request.getRecipientId() == null) {
            throw new ValidationException(
                    "recipientId is required"
            );
        }

        if (request.getTtlSeconds() == null) {
            throw new ValidationException(
                    "ttlSeconds is required"
            );
        }

        if (request.getOneTime() == null) {
            throw new ValidationException(
                    "oneTime is required"
            );
        }
    }
}