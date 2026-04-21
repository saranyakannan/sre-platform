package sre.agent.agents;

import com.google.adk.agents.LlmAgent;
import com.google.adk.tools.mcp.McpToolset;
import com.google.adk.tools.mcp.StdioServerParameters;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LogAnalyzerAgent {

    public static LlmAgent create(int days) {

        String projectId =
                System.getenv("GOOGLE_CLOUD_PROJECT");

        Map<String, String> env = new HashMap<>();
        env.put("GOOGLE_CLOUD_PROJECT", projectId);

        StdioServerParameters serverParams =
                StdioServerParameters.builder()
                        .command("npx")
                        .args(List.of(
                                "-y",
                                "@google-cloud/observability-mcp"
                        ))
                        .env(env)
                        .build();

        McpToolset mcpToolset = new McpToolset(
                serverParams.toServerParameters()
        );

        // ✅ Use days from frontend filter
        ZonedDateTime now =
                ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime startTime =
                now.minusDays(days);

        DateTimeFormatter fmt =
                DateTimeFormatter.ofPattern(
                        "yyyy-MM-dd'T'HH:mm:ss'Z'"
                );

        String nowStr       = now.format(fmt);
        String startTimeStr = startTime.format(fmt);

        String instruction = String.format("""
                You are an expert SRE Log Analyzer.
                
                CRITICAL: Do NOT call utcnow or any
                time/date tools. Timestamps are below.
                
                Project ID: %s
                Log name:   sre-error-service
                From:       %s
                Until:      %s
                
                IMMEDIATELY search logs using
                this exact filter:
                logName="projects/%s/logs/sre-error-service"
                AND severity>=ERROR
                AND timestamp>="%s"
                AND timestamp<="%s"
                
                Steps:
                1. Query Cloud Logging with the filter
                2. Identify all error types found
                3. Count how many times each occurred
                4. Extract stack trace snippets
                5. Note the affected service/endpoint
                6. Include any WARNING logs too
                
                Report your findings clearly.
                Do NOT ask for more information.
                Do NOT call any time tools.
                Start querying immediately.
                """,
                projectId,
                startTimeStr,
                nowStr,
                projectId,
                startTimeStr,
                nowStr
        );

        return LlmAgent.builder()
                .name("log_analyzer")
                .model("gemini-2.5-flash")
                .description(
                        "Analyzes Google Cloud Logging " +
                                "to find errors and root causes"
                )
                .instruction(instruction)
                .tools(List.of(mcpToolset))
                .outputKey("log_analysis")
                .build();
    }
}