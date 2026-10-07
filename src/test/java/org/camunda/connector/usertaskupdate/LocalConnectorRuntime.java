package org.camunda.connector.usertaskupdate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.concurrent.CountDownLatch;

@SpringBootApplication
public class LocalConnectorRuntime {

    public static void main(String[] args) throws InterruptedException {
        SpringApplication.run(LocalConnectorRuntime.class, args);
        // spring.main.web-application-type=none means there is no embedded web server thread to keep
        // the JVM alive, so without this the process exits right after startup instead of polling for jobs.
        new CountDownLatch(1).await();
    }
}
