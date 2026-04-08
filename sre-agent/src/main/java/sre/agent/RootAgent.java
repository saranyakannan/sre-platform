package sre.agent;

import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.LlmAgent;
import com.google.adk.agents.SequentialAgent;
import sre.agent.agents.LogAnalyzerAgent;
import sre.agent.agents.FixSuggesterAgent;
import sre.agent.agents.ResponseFormatterAgent;
import java.util.List;

public class RootAgent {

    public static BaseAgent ROOT_AGENT = create();

    public static BaseAgent create() {

        String projectId =
                System.getenv("GOOGLE_CLOUD_PROJECT");

        SequentialAgent sreWorkflow =
                SequentialAgent.builder()
                        .name("sre_workflow")
                        .description(
                                "Full SRE incident analysis workflow"
                        )
                        .subAgents(List.of(
                                LogAnalyzerAgent.create(),
                                FixSuggesterAgent.create(),
                                ResponseFormatterAgent.create()
                        ))
                        .build();

        String instruction = String.format("""
                You are an intelligent SRE Assistant
                for Google Cloud Platform.
                
                You are monitoring project: %s
                Log name to search: sre-error-service
                
                When a user reports an issue:
                1. Acknowledge briefly
                2. Say you will check Cloud Logging now
                3. IMMEDIATELY pass control to
                   sre_workflow - do not ask questions
                4. The workflow will:
                   - Query real Cloud Logging data
                   - Analyze what errors were found
                   - Suggest specific fixes
                   - Format a full incident report
                
                IMPORTANT: Always delegate to sre_workflow.
                Never answer directly yourself.
                The workflow has access to real log data.
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