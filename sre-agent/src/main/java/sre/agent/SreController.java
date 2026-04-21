package sre.agent;

import com.google.adk.events.Event;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.sessions.Session;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.logging.Logging;
import com.google.cloud.logging.Logging.EntryListOption;
import com.google.cloud.logging.LogEntry;
import com.google.cloud.logging.LoggingOptions;
import com.google.cloud.logging.Payload.JsonPayload;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import io.reactivex.rxjava3.core.Flowable;
import org.springframework.web.bind.annotation.*;
import sre.agent.agents.GitHubPrAgent;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class SreController {

    private Firestore getFirestore() {
        String projectId =
                System.getenv("GOOGLE_CLOUD_PROJECT");
        return FirestoreOptions.getDefaultInstance()
                .toBuilder()
                .setProjectId(projectId)
                .build()
                .getService();
    }

    // ── Run agent ─────────────────────────────────
    private String runAgent(String message) {
        try {
            InMemoryRunner runner = new InMemoryRunner(
                    RootAgent.ROOT_AGENT, "sre_assistant"
            );

            Session session = runner
                    .sessionService()
                    .createSession("sre_assistant", "user")
                    .blockingGet();

            Content content = Content.fromParts(
                    Part.fromText(message)
            );

            AtomicReference<String> response =
                    new AtomicReference<>("");

            Flowable<Event> events = runner.runAsync(
                    "user", session.id(), content
            );

            events.blockingForEach(event -> {
                if (event.finalResponse()) {
                    String text = event.stringifyContent();
                    if (text != null && !text.isEmpty()) {
                        response.set(text);
                    }
                }
            });

            return response.get();

        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    // ── POST /api/analyze ─────────────────────────
    // ── POST /api/analyze ─────────────────────────
    @PostMapping("/analyze")
    public Map<String, Object> analyze(
            @RequestBody Map<String, String> request
    ) {
        try {
            String message = request.getOrDefault(
                    "message",
                    "My user-api is throwing 500 errors. " +
                            "Analyze Cloud Logging and help!"
            );

            // ✅ Extract days from message
            int days = 1;
            if (message.contains(
                    "Time range to analyze: ")) {
                try {
                    String daysStr = message
                            .split("Time range to analyze: ")[1]
                            .split(" days")[0]
                            .trim();
                    days = Integer.parseInt(daysStr);
                } catch (Exception e) {
                    days = 1;
                }
            }

            String report = runAgent(message, days);
            saveIncident(message, report);

            return Map.of(
                    "success", true,
                    "report", report
            );

        } catch (Exception e) {
            return Map.of(
                    "success", false,
                    "error", e.getMessage()
            );
        }
    }

    // ── Run agent ─────────────────────────────────
    private String runAgent(String message, int days) {
        try {
            // ✅ Pass days to create fresh agent
            InMemoryRunner runner = new InMemoryRunner(
                    RootAgent.create(days), "sre_assistant"
            );

            Session session = runner
                    .sessionService()
                    .createSession("sre_assistant", "user")
                    .blockingGet();

            Content content = Content.fromParts(
                    Part.fromText(message)
            );

            AtomicReference<String> response =
                    new AtomicReference<>("");

            runner.runAsync(
                    "user", session.id(), content
            ).blockingForEach(event -> {
                if (event.finalResponse()) {
                    String text =
                            event.stringifyContent();
                    if (text != null
                            && !text.isEmpty()) {
                        response.set(text);
                    }
                }
            });

            return response.get();

        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    // ── GET /api/errors?days=1 ────────────────────
    @GetMapping("/errors")
    public Map<String, Object> getErrors(
            @RequestParam(defaultValue = "1") int days
    ) {
        try {
            String projectId =
                    System.getenv("GOOGLE_CLOUD_PROJECT");

            Logging logging = LoggingOptions
                    .getDefaultInstance()
                    .toBuilder()
                    .setProjectId(projectId)
                    .build()
                    .getService();

            long cutoffMs = System.currentTimeMillis()
                    - (long) days * 24 * 60 * 60 * 1000;

            String timestamp = new SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss'Z'"
            ).format(new Date(cutoffMs));

            String filter =
                    "logName=\"projects/" + projectId +
                            "/logs/sre-error-service\"" +
                            " AND severity>=\"ERROR\"" +
                            " AND timestamp>=\"" + timestamp + "\"";

            Iterable<LogEntry> entries =
                    logging.listLogEntries(
                            EntryListOption.filter(filter),
                            EntryListOption.pageSize(200)
                    ).iterateAll();

            // Deduplicate by errorType
            Map<String, Map<String, Object>> errorMap =
                    new LinkedHashMap<>();

            for (LogEntry entry : entries) {
                String errorType = "Unknown";
                String msg = "";
                String severity = entry.getSeverity()
                        .toString();

                if (entry.getPayload()
                        instanceof JsonPayload) {
                    // ✅ Use Map<String, Object>
                    Map<String, Object> data =
                            ((JsonPayload) entry.getPayload())
                                    .getDataAsMap();

                    if (data.containsKey("errorType")) {
                        errorType = data.get("errorType")
                                .toString();
                    }
                    if (data.containsKey("message")) {
                        msg = data.get("message")
                                .toString();
                    }
                }

                if (!errorMap.containsKey(errorType)) {
                    Map<String, Object> e = new HashMap<>();
                    e.put("type", errorType);
                    e.put("severity",
                            severity.equalsIgnoreCase("ERROR")
                                    ? "error" : "critical");
                    e.put("msg", msg.length() > 100
                            ? msg.substring(0, 100) + "..."
                            : msg);
                    e.put("count", 1);
                    e.put("lastSeen",
                            entry.getInstantTimestamp()
                                    .toString());
                    errorMap.put(errorType, e);
                } else {
                    Map<String, Object> existing =
                            errorMap.get(errorType);
                    existing.put("count",
                            (int) existing.get("count") + 1);
                }
            }

            logging.close();

            List<Map<String, Object>> errors =
                    new ArrayList<>(errorMap.values());

            return Map.of(
                    "success", true,
                    "errors", errors,
                    "count", errors.size()
            );

        } catch (Exception e) {
            return Map.of(
                    "success", false,
                    "error", e.getMessage(),
                    "errors", List.of()
            );
        }
    }

    // ── GET /api/incidents ────────────────────────
    @GetMapping("/incidents")
    public Map<String, Object> getIncidents() {
        try {
            Firestore db = getFirestore();
            List<Map<String, Object>> incidents =
                    new ArrayList<>();

            List<QueryDocumentSnapshot> docs =
                    db.collection("incidents")
                            .orderBy("timestamp",
                                    com.google.cloud.firestore
                                            .Query.Direction.DESCENDING)
                            .limit(100)
                            .get()
                            .get()
                            .getDocuments();

            for (QueryDocumentSnapshot doc : docs) {
                Map<String, Object> inc =
                        new HashMap<>(doc.getData());
                inc.put("id", doc.getId());
                incidents.add(inc);
            }

            db.close();

            return Map.of(
                    "success", true,
                    "incidents", incidents
            );

        } catch (Exception e) {
            return Map.of(
                    "success", false,
                    "error", e.getMessage(),
                    "incidents", List.of()
            );
        }
    }

    // ── GET /api/incidents/{errorType} ───────────────
    @GetMapping("/incidents/{errorType}")
    public Map<String, Object> getIncidentByType(
            @PathVariable String errorType
    ) {
        try {
            Firestore db = getFirestore();

            List<QueryDocumentSnapshot> docs =
                    db.collection("incidents")
                            .whereEqualTo("errorType", errorType)
                            .orderBy("timestamp",
                                    com.google.cloud.firestore
                                            .Query.Direction.DESCENDING)
                            .limit(1)
                            .get()
                            .get()
                            .getDocuments();

            db.close();

            if (!docs.isEmpty()) {
                Map<String, Object> inc =
                        new HashMap<>(docs.get(0).getData());
                inc.put("id", docs.get(0).getId());
                return Map.of(
                        "success", true,
                        "found", true,
                        "incident", inc
                );
            }

            return Map.of(
                    "success", true,
                    "found", false
            );

        } catch (Exception e) {
            return Map.of(
                    "success", false,
                    "found", false,
                    "error", e.getMessage()
            );
        }
    }

    // ── Save incident to Firestore ────────────────
    private void saveIncident(
            String message,
            String report
    ) {
        try {
            Firestore db = getFirestore();

            String errorType = "Unknown";
            String[] types = {
                    "NullPointerException",
                    "OutOfMemoryError",
                    "DatabaseConnectionError",
                    "SocketTimeoutException",
                    "IllegalArgumentException",
                    "StackOverflowError",
                    "ArrayIndexOutOfBoundsException"
            };
            for (String t : types) {
                if (message.contains(t)) {
                    errorType = t;
                    break;
                }
            }

            String severity =
                    report.contains("CRITICAL") ? "CRITICAL" :
                            report.contains("HIGH")     ? "HIGH"     :
                                    "MEDIUM";

            Map<String, Object> incident = new HashMap<>();
            incident.put("errorType", errorType);
            incident.put("severity", severity);
            incident.put("report", report);
            incident.put("timestamp",
                    new Date().toInstant().toString());

            db.collection("incidents").add(incident).get();
            db.close();

        } catch (Exception e) {
            System.err.println(
                    "Failed to save incident: " +
                            e.getMessage()
            );
        }
    }

    // ── POST /api/raise-pr ────────────────────────────
    @PostMapping("/raise-pr")
    public Map<String, Object> raisePr(
            @RequestBody Map<String, String> request
    ) {
        try {
            String errorType = request.getOrDefault(
                    "errorType", "Unknown"
            );
            String report = request.getOrDefault(
                    "report", ""
            );

            String message = String.format(
                    "errorType: %s\nreport:\n%s",
                    errorType, report
            );

            InMemoryRunner runner = new InMemoryRunner(
                    GitHubPrAgent.create(),
                    "github_pr_agent"
            );

            Session session = runner
                    .sessionService()
                    .createSession(
                            "github_pr_agent", "user"
                    )
                    .blockingGet();

            Content content = Content.fromParts(
                    Part.fromText(message)
            );

            AtomicReference<String> response =
                    new AtomicReference<>("");

            runner.runAsync(
                    "user", session.id(), content
            ).blockingForEach(event -> {
                if (event.finalResponse()) {
                    String text =
                            event.stringifyContent();
                    if (text != null
                            && !text.isEmpty()) {
                        response.set(text);
                    }
                }
            });

            String result = response.get();

            // Extract PR URL and file changed from response
            String prUrl = "";
            String fileChanged = "";

            for (String line : result.split("\n")) {
                if (line.trim().startsWith("PR_URL:")) {
                    prUrl = line.replace("PR_URL:", "").trim();
                }
                if (line.trim().startsWith("FILE:")) {
                    fileChanged = line.replace("FILE:", "").trim();
                }
            }

            return Map.of(
                    "success", true,
                    "result", result,
                    "prUrl", prUrl,
                    "fileChanged", fileChanged
            );

        } catch (Exception e) {
            return Map.of(
                    "success", false,
                    "error", e.getMessage()
            );
        }
    }

    // ── GET /api/health ───────────────────────────
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of(
                "status", "UP",
                "service", "incidentiq"
        );
    }
}