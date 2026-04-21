package sre.agent;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SreAgentApplication {

    public static void main(String[] args) throws Exception {

        // Load .env file if it exists (local dev only)
        try {
            Dotenv dotenv = Dotenv.configure()
                    .ignoreIfMissing()  // won't fail on Cloud Run
                    .load();
            dotenv.entries().forEach(e ->
                    System.setProperty(
                            e.getKey(), e.getValue()
                    )
            );
        } catch (Exception e) {
            System.out.println(
                    "No .env file found, " +
                            "using system env vars"
            );
        }

        String port = System.getenv("PORT");
        if (port == null || port.isEmpty()) {
            port = "8080";
        }
        System.setProperty("server.port", port);

        SpringApplication.run(
                SreAgentApplication.class, args
        );
    }
}