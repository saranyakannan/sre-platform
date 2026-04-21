package sre.agent;

import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.LlmAgent;
import com.google.adk.agents.SequentialAgent;
import sre.agent.agents.LogAnalyzerAgent;
import sre.agent.agents.FixSuggesterAgent;
import sre.agent.agents.ResponseFormatterAgent;
import java.util.List;

public class RootAgent {

    // ✅ Default kept for backward compat
    public static BaseAgent ROOT_AGENT = create(1);

    public static BaseAgent create(int days) {

        String projectId =
                System.getenv("GOOGLE_CLOUD_PROJECT");

        // ✅ Pass days to LogAnalyzerAgent
        SequentialAgent sreWorkflow =
                SequentialAgent.builder()
                        .name("sre_workflow")
                        .description(
                                "SRE incident analysis workflow"
                        )
                        .subAgents(List.of(
                                LogAnalyzerAgent.create(days),
                                FixSuggesterAgent.create(),
                                ResponseFormatterAgent.create()
                        ))
                        .build();

        String instruction = String.format("""
                You are an intelligent SRE Assistant
                for Google Cloud Platform.
                
                Project: %s
                Log name: sre-error-service
                
                CRITICAL: Never call utcnow, get_time,
                or any date/time tools. Timestamps are
                already provided to the sub-agents.
                
                When user reports an error →
                delegate to sre_workflow immediately.
                
                Do not answer directly.
                Always delegate to sre_workflow.
                """,
                projectId
        );

        return LlmAgent.builder()
                .name("sre_assistant")
                .model("gemini-2.5-flash")
                .description("SRE Intelligence Agent")
                .instruction(instruction)
                .subAgents(List.of(sreWorkflow))
                .build();
    }
}