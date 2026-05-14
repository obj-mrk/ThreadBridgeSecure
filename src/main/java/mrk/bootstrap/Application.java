package mrk.bootstrap;

import com.sun.net.httpserver.HttpServer;

public class Application {
    private final HttpServer server;

    public Application(HttpServer server) {
        this.server = server;
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(1);
    }

}
