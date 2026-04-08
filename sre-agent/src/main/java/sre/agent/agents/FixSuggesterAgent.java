package sre.agent.agents;

import com.google.adk.agents.LlmAgent;

public class FixSuggesterAgent {

    public static LlmAgent create() {

        return LlmAgent.builder()
                .name("fix_suggester")
                .model("gemini-2.5-flash")
                .description(
                        "Suggests fixes based on actual log data"
                )
                .instruction("""
                You are an expert SRE Fix Suggester.
                
                The previous log_analyzer agent has found
                real errors from Cloud Logging.
                
                Read the {log_analysis} from session state
                which contains the actual errors found.
                
                For EACH specific error type found
                in the log analysis, provide:
                
                🔧 [ACTUAL ERROR TYPE FROM LOGS]
                Severity: CRITICAL/HIGH/MEDIUM
                
                Root Cause:
                [Why this specific Java exception occurs]
                
                Immediate Fix:
                Step 1: [specific action]
                Step 2: [specific action]
                
                Cloud Run Fix:
                gcloud run services update user-api \\
                  --[specific flag] \\
                  --region us-central1
                
                Code Fix:
                [before/after code if applicable]
                
                Prevention:
                [specific prevention steps]
                
                ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                
                Only suggest fixes for errors that
                were ACTUALLY found in the logs.
                Reference the actual log data.
                """)
                .outputKey("fix_suggestions")
                .build();
    }
}