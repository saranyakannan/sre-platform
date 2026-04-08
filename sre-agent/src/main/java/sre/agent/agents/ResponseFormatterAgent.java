package sre.agent.agents;

import com.google.adk.agents.LlmAgent;

public class ResponseFormatterAgent {

    public static LlmAgent create() {

        return LlmAgent.builder()
                .name("response_formatter")
                .model("gemini-2.5-flash")
                .description(
                        "Formats the final SRE incident report"
                )
                .instruction("""
                You are an SRE Report Formatter.
                
                Read {log_analysis} and {fix_suggestions}
                from session state.
                
                Create this exact structured report:
                
                ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                🚨 SRE INCIDENT REPORT
                ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                
                📋 INCIDENT SUMMARY
                Service:    user-api
                Severity:   [based on worst error found]
                Errors:     [total count from log_analysis]
                Warnings:   [total count from log_analysis]
                Status:     ACTIVE 🚨
                
                🔍 ERRORS DETECTED FROM CLOUD LOGGING
                [List ONLY errors actually found in logs]
                
                📊 LOG EVIDENCE
                [Paste actual log entries from log_analysis]
                
                🔧 RECOMMENDED FIXES
                [From fix_suggestions - one per error type]
                
                ⚡ IMMEDIATE ACTIONS (Do These NOW)
                1. [most urgent action]
                2. [second action]
                3. [third action]
                
                🛡️ PREVENTION
                [How to prevent recurrence]
                
                ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                
                IMPORTANT: Only report what was actually
                found in the real Cloud Logging data.
                Do not add generic errors not in the logs.
                """)
                .outputKey("incident_report")
                .build();
    }
}