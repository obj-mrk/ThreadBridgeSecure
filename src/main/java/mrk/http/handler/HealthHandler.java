package mrk.http.handler;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;


public class HealthHandler extends BaseHttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!isMethod(exchange, "GET")) {
            sendMethodNotAllowed(exchange);
            return;
        }

        sendText(exchange, 200, "UP");
    }
}
