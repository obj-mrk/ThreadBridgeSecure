package mrk;

import mrk.bootstrap.Application;
import mrk.bootstrap.DependencyFactory;
import mrk.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws IOException {
        AppConfig appConfig = new AppConfig();
        DependencyFactory dependencyFactory = new DependencyFactory(appConfig);

        Application application = dependencyFactory.createApplication();

        Runtime.getRuntime().addShutdownHook(new Thread(application::stop));
        application.start();

        log.info(
                "{} started on {}:{}",
                appConfig.getApplicationName(),
                appConfig.getServerHost(),
                appConfig.getServerPort()
        );
    }
}