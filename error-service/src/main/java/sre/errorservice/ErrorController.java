package sre.errorservice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ErrorController {

    @Autowired
    private CloudLoggingService loggingService;

    // ── Health Check ───────────────────────────────────
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "user-api");
        response.put("port", "8081");
        return ResponseEntity.ok(response);
    }

    // ── NullPointerException ───────────────────────────
    // Real: calls .length() on null object
    @GetMapping("/error/nullpointer")
    public ResponseEntity<Map<String, Object>> nullPointer() {
        try {
            // Simulates fetching user from DB
            // returning null
            Map<String, String> user = getUserFromDb(999);

            // This naturally throws NullPointerException
            // because user is null
            String name = user.get("name").toUpperCase();
            System.out.println(name);

        } catch (NullPointerException e) {
            String stack = getStackTrace(e);
            loggingService.logError(
                    "NullPointerException",
                    "HTTP 500: NullPointerException - " +
                            "Cannot invoke String.toUpperCase() " +
                            "because return value of " +
                            "Map.get(Object) is null. " +
                            "User ID 999 not found in database.",
                    stack,
                    "/api/error/nullpointer"
            );
            return buildErrorResponse(
                    "NullPointerException",
                    "User object returned null from database"
            );
        }
        return null;
    }

    // ── OutOfMemoryError ───────────────────────────────
    // Real: actually fills up JVM heap memory
    @GetMapping("/error/outofmemory")
    public ResponseEntity<Map<String, Object>> outOfMemory() {
        try {
            // Simulates a memory leak in order processing
            // Keeps adding large byte arrays until OOM
            List<byte[]> memoryLeak = new ArrayList<>();
            while (true) {
                // Each iteration adds 1MB to heap
                memoryLeak.add(new byte[1024 * 1024]);
            }

        } catch (OutOfMemoryError e) {
            String stack = getStackTrace(e);
            loggingService.logError(
                    "OutOfMemoryError",
                    "HTTP 500: OutOfMemoryError - " +
                            "Java heap space exhausted. " +
                            "Memory leak detected in " +
                            "OrderProcessor while loading " +
                            "all orders into memory.",
                    stack,
                    "/api/error/outofmemory"
            );
            loggingService.logWarning(
                    "Critical: JVM heap space exhausted. " +
                            "Service needs immediate restart.",
                    "/api/error/outofmemory"
            );
            return buildErrorResponse(
                    "OutOfMemoryError",
                    "Java heap space exhausted - " +
                            "memory leak in OrderProcessor"
            );
        }
        return null;
    }

    // ── Database Error ─────────────────────────────────
    // Real: actually tries to connect to
    // a non-existent database
    @GetMapping("/error/database")
    public ResponseEntity<Map<String, Object>> database() {
        try {
            // Tries to load JDBC driver and connect
            // to a non-existent database
            // This naturally throws ClassNotFoundException
            // or connection refused
            Class.forName("org.postgresql.Driver");

            java.sql.Connection conn =
                    java.sql.DriverManager.getConnection(
                            "jdbc:postgresql://localhost:5432/userdb",
                            "admin",
                            "password"
                    );
            conn.close();

        } catch (ClassNotFoundException e) {
            // PostgreSQL driver not found
            String stack = getStackTrace(e);
            loggingService.logError(
                    "DatabaseDriverError",
                    "HTTP 500: ClassNotFoundException - " +
                            "PostgreSQL JDBC driver not found. " +
                            "Database connection cannot be " +
                            "established for userdb.",
                    stack,
                    "/api/error/database"
            );
            loggingService.logWarning(
                    "Database unreachable: " +
                            "Connection pool will be exhausted soon",
                    "/api/error/database"
            );
            return buildErrorResponse(
                    "DatabaseDriverError",
                    "PostgreSQL driver not found - " +
                            "check classpath configuration"
            );

        } catch (java.sql.SQLException e) {
            // DB is unreachable — connection refused
            String stack = getStackTrace(e);
            loggingService.logError(
                    "DatabaseConnectionError",
                    "HTTP 500: SQLException - " +
                            e.getMessage() +
                            " | Database userdb unreachable on " +
                            "localhost:5432",
                    stack,
                    "/api/error/database"
            );
            loggingService.logWarning(
                    "Slow query fallback: " +
                            "3200ms timeout for getUserOrders()",
                    "/api/error/database"
            );
            return buildErrorResponse(
                    "DatabaseConnectionError",
                    "Cannot connect to database: " +
                            e.getMessage()
            );
        }
        return null;
    }

    // ── Timeout Error ──────────────────────────────────
    // Real: actually waits then times out
    @GetMapping("/error/timeout")
    public ResponseEntity<Map<String, Object>> timeout() {
        try {
            // Tries to open a connection to a
            // non-existent host with a very short timeout
            // This naturally throws SocketTimeoutException
            java.net.URL url = new java.net.URL(
                    "http://payment-api.internal:9090/charge"
            );
            java.net.HttpURLConnection connection =
                    (java.net.HttpURLConnection)
                            url.openConnection();

            // Set very short timeout to force timeout
            connection.setConnectTimeout(100);
            connection.setReadTimeout(100);
            connection.getInputStream();

        } catch (java.net.SocketTimeoutException e) {
            String stack = getStackTrace(e);
            loggingService.logError(
                    "SocketTimeoutException",
                    "HTTP 500: SocketTimeoutException - " +
                            "PaymentAPI at payment-api.internal:9090 " +
                            "did not respond within timeout. " +
                            "Affected: CheckoutService.processPayment()",
                    stack,
                    "/api/error/timeout"
            );
            return buildErrorResponse(
                    "SocketTimeoutException",
                    "PaymentAPI connection timed out"
            );

        } catch (java.net.ConnectException e) {
            // Connection refused - also a real network error
            String stack = getStackTrace(e);
            loggingService.logError(
                    "ConnectionRefused",
                    "HTTP 500: ConnectException - " +
                            "PaymentAPI connection refused at " +
                            "payment-api.internal:9090. " +
                            "Service may be down.",
                    stack,
                    "/api/error/timeout"
            );
            return buildErrorResponse(
                    "ConnectionRefused",
                    "PaymentAPI is unreachable - " +
                            "connection refused"
            );

        } catch (Exception e) {
            String stack = getStackTrace(e);
            loggingService.logError(
                    "NetworkError",
                    "HTTP 500: Network error - " +
                            e.getMessage(),
                    stack,
                    "/api/error/timeout"
            );
            return buildErrorResponse(
                    "NetworkError",
                    e.getMessage()
            );
        }
        return null;
    }

    // ── Auth Error ─────────────────────────────────────
    // Real: actually tries to decode
    // an invalid JWT token
    @GetMapping("/error/auth")
    public ResponseEntity<Map<String, Object>> auth() {
        try {
            // Tries to decode an invalid
            // Base64 JWT token
            // This naturally throws IllegalArgumentException
            String fakeToken =
                    "Bearer eyInvalidToken!!@#$%^&*";

            // Strip "Bearer " prefix
            String tokenValue =
                    fakeToken.substring(7);

            // This naturally throws
            // IllegalArgumentException for invalid base64
            byte[] decodedBytes = java.util.Base64
                    .getDecoder()
                    .decode(tokenValue);

            // Try to parse as JSON
            // throws exception for invalid format
            String decoded = new String(decodedBytes);
            if (!decoded.contains("userId")) {
                throw new SecurityException(
                        "JWT payload missing required " +
                                "claim: userId"
                );
            }c

        } catch (IllegalArgumentException e) {
            // Real exception from Base64 decoder
            String stack = getStackTrace(e);
            loggingService.logError(
                    "InvalidTokenError",
                    "HTTP 500: IllegalArgumentException - " +
                            "JWT token contains invalid characters. " +
                            "Possible token tampering detected in " +
                            "AuthService.validateToken()",
                    stack,
                    "/api/error/auth"
            );
            return buildErrorResponse(
                    "InvalidTokenError",
                    "JWT token is malformed or tampered"
            );

        } catch (SecurityException e) {
            String stack = getStackTrace(e);
            loggingService.logError(
                    "SecurityException",
                    "HTTP 500: SecurityException - " +
                            e.getMessage(),
                    stack,
                    "/api/error/auth"
            );
            return buildErrorResponse(
                    "SecurityException",
                    e.getMessage()
            );
        }
        return null;
    }

    // ── Stack Overflow ─────────────────────────────────
    // Real: actually causes infinite recursion
    @GetMapping("/error/stackoverflow")
    public ResponseEntity<Map<String, Object>> stackOverflow() {
        try {
            // Real StackOverflowError
            // via infinite recursion
            infiniteRecursion(0);

        } catch (StackOverflowError e) {
            String stack = getStackTrace(e);
            loggingService.logError(
                    "StackOverflowError",
                    "HTTP 500: StackOverflowError - " +
                            "Infinite recursion detected in " +
                            "CategoryService.getSubCategories(). " +
                            "Circular reference in category tree.",
                    stack,
                    "/api/error/stackoverflow"
            );
            return buildErrorResponse(
                    "StackOverflowError",
                    "Infinite recursion in category tree"
            );
        }
        return null;
    }

    // ── Array Index Out of Bounds ──────────────────────
    // Real: actually accesses invalid array index
    @GetMapping("/error/arrayindex")
    public ResponseEntity<Map<String, Object>> arrayIndex() {
        try {
            // Real ArrayIndexOutOfBoundsException
            int[] reportData = {100, 200, 300};

            // Tries to access index 10
            // which doesn't exist
            int value = reportData[10];
            System.out.println(value);

        } catch (ArrayIndexOutOfBoundsException e) {
            String stack = getStackTrace(e);
            loggingService.logError(
                    "ArrayIndexOutOfBoundsException",
                    "HTTP 500: ArrayIndexOutOfBoundsException - " +
                            "Index 10 out of bounds for length 3. " +
                            "Report pagination logic error in " +
                            "ReportGenerator.getPage()",
                    stack,
                    "/api/error/arrayindex"
            );
            return buildErrorResponse(
                    "ArrayIndexOutOfBoundsException",
                    "Array index 10 out of bounds for length 3"
            );
        }
        return null;
    }

    // ── Trigger ALL errors at once ─────────────────────
    @GetMapping("/errors/trigger-all")
    public ResponseEntity<Map<String, Object>> triggerAll() {
        System.out.println(
                "🚨 Triggering all error types..."
        );

        nullPointer();
        outOfMemory();
        database();
        timeout();
        auth();
        stackOverflow();
        arrayIndex();

        loggingService.logWarning(
                "Rate limit approaching: 850/1000 req/min",
                "/api/errors/trigger-all"
        );
        loggingService.logWarning(
                "Cache miss rate high: 78% in last 5 min",
                "/api/errors/trigger-all"
        );

        Map<String, Object> response = new HashMap<>();
        response.put("status", "triggered");
        response.put("message",
                "All 7 error types triggered and " +
                        "logged to Cloud Logging"
        );
        response.put("errorsLogged", 7);
        response.put("warningsLogged", 4);
        response.put("viewLogs",
                "https://console.cloud.google.com/logs"
        );

        System.out.println(
                "✅ All errors logged to Cloud Logging!"
        );

        return ResponseEntity.ok(response);
    }

    // ── Helper: simulate null DB response ─────────────
    private Map<String, String> getUserFromDb(int userId) {
        // Simulates DB returning null for unknown user
        if (userId == 999) return null;
        Map<String, String> user = new HashMap<>();
        user.put("name", "John Doe");
        return user;
    }

    // ── Helper: infinite recursion ─────────────────────
    private void infiniteRecursion(int depth) {
        // Add a recursion guard to prevent StackOverflowError
        if (depth > 2000) { // A reasonable limit to demonstrate the fix
            return;
        }
        // Calls itself
        infiniteRecursion(depth + 1);
    }

    // ── Helper: extract real stack trace ──────────────
    private String getStackTrace(Throwable e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    // ── Helper: build error response ──────────────────
    private ResponseEntity<Map<String, Object>>
    buildErrorResponse(
            String errorType,
            String message
    ) {
        Map<String, Object> error = new HashMap<>();
        error.put("status", 500);
        error.put("error", errorType);
        error.put("message", message);
        error.put("service", "user-api");
        error.put("logged", true);
        error.put("logDestination", "Google Cloud Logging");
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error);
    }
