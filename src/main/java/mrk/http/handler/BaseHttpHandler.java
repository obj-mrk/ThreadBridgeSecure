package mrk.http.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import mrk.application.exception.AuthenticationException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public abstract class BaseHttpHandler implements HttpHandler {
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    protected String readBody(HttpExchange exchange) throws IOException {
        try (InputStream inputStream = exchange.getRequestBody()) {
            return new String(
                    inputStream.readAllBytes(),
                    StandardCharsets.UTF_8
            );
        }
    }

    protected String extractBearerToken(HttpExchange exchange) {
        String authorization = exchange
                .getRequestHeaders()
                .getFirst(AUTHORIZATION_HEADER);

        if (authorization == null ||
                !authorization.regionMatches(
                        true,
                        0,
                        BEARER_PREFIX,
                        0,
                        BEARER_PREFIX.length()
                )) {

            throw new AuthenticationException(
                    "Invalid authentication credentials"
            );
        }

        String token = authorization
                .substring(BEARER_PREFIX.length())
                .trim();

        if (token.isBlank()) {
            throw new AuthenticationException(
                    "Invalid authentication credentials"
            );
        }

        return token;
    }

    protected void sendText(HttpExchange exchange, int statusCode, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
                "Content-Type",
                "text/plain; charset=utf-8"
        );

        exchange.sendResponseHeaders(statusCode, bytes.length);

        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }

    protected void sendJson(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] responseBytes = json.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
                "Content-Type",
                "application/json; charset=utf-8"
        );

        exchange.sendResponseHeaders(
                statusCode,
                responseBytes.length
        );

        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(responseBytes);
        }
    }

    protected boolean isMethod(HttpExchange exchange, String method) {
        return method.equalsIgnoreCase(
                exchange.getRequestMethod()
        );
    }

    protected void sendMethodNotAllowed(HttpExchange exchange) throws IOException {
        sendText(exchange, 405, "Method Not Allowed");
    }

    protected void sendInternalServerError(HttpExchange exchange) throws IOException {
        sendText(exchange, 500, "Internal Server Error");
    }
}