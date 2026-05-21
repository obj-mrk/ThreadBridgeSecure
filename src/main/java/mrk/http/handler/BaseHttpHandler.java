package mrk.http.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public abstract class BaseHttpHandler implements HttpHandler {
    protected String readBody(HttpExchange he) throws IOException {
        try (InputStream is = he.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    protected void sendText(HttpExchange he, int statusCode, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);

        he.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        he.sendResponseHeaders(statusCode, bytes.length);

        try (OutputStream os = he.getResponseBody()) {
            os.write(bytes);
        }
    }

    protected void sendJson(HttpExchange ex, int statusCode, String json) throws IOException {
        byte[] responseBytes = json.getBytes(StandardCharsets.UTF_8);

        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(statusCode, responseBytes.length);

        try (OutputStream outputStream = ex.getResponseBody()) {
            outputStream.write(responseBytes);
        }
    }

    protected boolean isMethod(HttpExchange ex, String method) {
        return method.equalsIgnoreCase(ex.getRequestMethod());
    }

    protected void sendMethodNotAllowed(HttpExchange ex) throws IOException {
        sendText(ex, 405, "Method Not Allowed");
    }

    protected void sendInternalServerError(HttpExchange ex) throws IOException {
        sendText(ex, 500, "Internal Server Error");
    }
}
