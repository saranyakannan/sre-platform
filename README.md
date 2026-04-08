# IncidentIQ — SRE Intelligence Agent

AI-powered incident detection and analysis
built on Google ADK, MCP, and Cloud Run.

## Architecture
- **RootAgent** — orchestrates the workflow
- **LogAnalyzerAgent** — queries Cloud Logging via MCP
- **FixSuggesterAgent** — generates fixes using Gemini
- **ResponseFormatterAgent** — formats the report
- **Firestore** — stores incident history

## Tech Stack
- Google ADK Java 1.0.0-rc.1
- Gemini 2.5 Flash via Vertex AI
- Cloud Observability MCP Server
- Firestore (incident storage)
- Spring Boot (REST API + UI)
- Cloud Run (deployment)

## Live Demo
UI: https://sre-agent-263830372809.us-central1.run.app

## How It Works
1. Error Feed loads real errors from Cloud Logging
2. Click Analyze on any error row
3. Agent queries logs via MCP → Gemini analyzes
4. Full report shown in UI
5. Incident saved to Firestore automatically
