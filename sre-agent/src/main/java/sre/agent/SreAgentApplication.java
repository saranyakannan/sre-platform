package sre.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SreAgentApplication {

    public static void main(String[] args) throws Exception {

        String port = System.getenv("PORT");
        if (port == null || port.isEmpty()) {
            port = "8080";
        }

        System.setProperty("server.port", port);

        System.out.println(
                "🤖 Starting IncidentIQ on port " + port
        );

        // ← No more AdkWebServer.start()!
        // Spring Boot handles everything
        SpringApplication.run(
                SreAgentApplication.class, args
        );
    }
}