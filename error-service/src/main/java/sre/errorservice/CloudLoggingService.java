package sre.errorservice;

import com.google.cloud.logging.LogEntry;
import com.google.cloud.logging.Logging;
import com.google.cloud.logging.LoggingOptions;
import com.google.cloud.logging.Payload.JsonPayload;
import com.google.cloud.logging.Severity;
import com.google.cloud.MonitoredResource;
import org.springframework.stereotype.Service;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
public class CloudLoggingService {

    private final Logging logging;
    private static final String LOG_NAME = "sre-error-service";
    private static final String SERVICE_NAME = "user-api";
    private static final String PROJECT_ID =
            System.getenv("GOOGLE_CLOUD_PROJECT");

    public CloudLoggingService() {
        this.logging = LoggingOptions
                .getDefaultInstance()
                .getService();
        System.out.println(
                "✅ Cloud Logging initialized for project: "
                        + PROJECT_ID
        );
    }

    public void logError(
            String errorType,
            String message,
            String stackTrace,
            String endpoint
    ) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("severity", "ERROR");
            payload.put("message", message);
            payload.put("errorType", errorType);
            payload.put("stackTrace", stackTrace);
            payload.put("serviceName", SERVICE_NAME);
            payload.put("endpoint", endpoint);
            payload.put("httpStatusCode", 500);
            payload.put("environment", "production");

            MonitoredResource resource = MonitoredResource
                    .newBuilder("cloud_run_revision")
                    .addLabel("service_name", SERVICE_NAME)
                    .addLabel("revision_name", SERVICE_NAME + "-v1")
                    .addLabel("location", "us-central1")
                    .addLabel("project_id", PROJECT_ID)
                    .build();

            LogEntry entry = LogEntry
                    .newBuilder(JsonPayload.of(payload))
                    .setSeverity(Severity.ERROR)
                    .setLogName(LOG_NAME)
                    .setResource(resource)
                    .build();

            logging.write(Collections.singleton(entry));
            logging.flush();

            System.out.println(
                    "📝 Logged ERROR to Cloud Logging: "
                            + errorType
            );

        } catch (Exception e) {
            System.err.println(
                    "❌ Failed to write to Cloud Logging: "
                            + e.getMessage()
            );
        }
    }

    public void logWarning(
            String message,
            String endpoint
    ) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("severity", "WARNING");
            payload.put("message", message);
            payload.put("serviceName", SERVICE_NAME);
            payload.put("endpoint", endpoint);

            MonitoredResource resource = MonitoredResource
                    .newBuilder("cloud_run_revision")
                    .addLabel("service_name", SERVICE_NAME)
                    .addLabel("revision_name", SERVICE_NAME + "-v1")
                    .addLabel("location", "us-central1")
                    .addLabel("project_id", PROJECT_ID)
                    .build();

            LogEntry entry = LogEntry
                    .newBuilder(JsonPayload.of(payload))
                    .setSeverity(Severity.WARNING)
                    .setLogName(LOG_NAME)
                    .setResource(resource)
                    .build();

            logging.write(Collections.singleton(entry));
            logging.flush();

            System.out.println(
                    "⚠️  Logged WARNING to Cloud Logging: "
                            + message
            );

        } catch (Exception e) {
            System.err.println(
                    "❌ Failed to write warning: "
                            + e.getMessage()
            );
        }
    }
}