// ── Analyze a specific error ──────────────────────
async function analyzeError(type, msg, count, lastSeen) {
    currentErrorType     = type;
    currentErrorCount    = count;
    currentErrorLastSeen = lastSeen;

    const days = document.getElementById("filter-time").value;

    // Show panel
    const panel = document.getElementById("diagnostics-panel");
    panel.classList.remove("hidden");
    panel.classList.add("flex");

    document.getElementById("panel-footer").classList.add("hidden");

    // Show loading
    document.getElementById("panel-loading").classList.remove("hidden");
    document.getElementById("panel-loading").classList.add("flex");
    document.getElementById("panel-report").classList.add("hidden");
    document.getElementById("saved-badge").classList.add("hidden");
    document.getElementById("saved-badge").classList.remove("flex");

    document.getElementById("panel-meta").innerHTML = `
        <span class="text-[10px] font-bold text-primary bg-primary/10
                     px-2 py-0.5 rounded uppercase tracking-wider analyzing">
            Checking Firestore...
        </span>`;

    // ✅ Check Firestore first — errorType is the unique key
    try {
        const cached = await fetch(
            `/api/incidents/${encodeURIComponent(type)}`
        );
        const cachedData = await cached.json();

        if (cachedData.success && cachedData.found) {
            // Found in Firestore — show instantly, no LLM
            showPanelReport(cachedData.incident.report, true);
            return;
        }
    } catch (e) {
        // Cache check failed — fall through to LLM
        console.warn("Cache check failed:", e.message);
    }

    // Not in Firestore — run full LLM pipeline once
    document.getElementById("panel-meta").innerHTML = `
        <span class="text-[10px] font-bold text-primary bg-primary/10
                     px-2 py-0.5 rounded uppercase tracking-wider analyzing">
            Analyzing ${type}...
        </span>`;

    const message =
        `My user-api service is throwing ${type}. ` +
        `Message: ${msg}. ` +
        `This has occurred ${count} times. ` +
        `Last seen: ${lastSeen}. ` +
        `Time range to analyze: ${days} days. ` +
        `Please analyze Cloud Logging for this specific ` +
        `error and provide a detailed fix.`;

    try {
        const res = await fetch("/api/analyze", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ message })
        });

        const data = await res.json();

        if (data.success) {
            showPanelReport(data.report, false);
        } else {
            closePanel();
            showToast("Analysis failed: " + data.error);
        }
    } catch (e) {
        closePanel();
        showToast("Error: " + e.message);
    }
}

// ── Show report in panel ──────────────────────────
function showPanelReport(report, fromCache) {
    currentReport = report;

    // Hide loading, show report
    document.getElementById("panel-loading").classList.add("hidden");
    document.getElementById("panel-loading").classList.remove("flex");
    document.getElementById("panel-report").classList.remove("hidden");

    // Stats — real data from Cloud Logging
    document.getElementById("report-error-count")
        .textContent = currentErrorCount + "x";
    document.getElementById("report-error-type")
        .textContent = currentErrorType;
    document.getElementById("report-last-seen")
        .textContent = currentErrorLastSeen
            ? new Date(currentErrorLastSeen).toLocaleString()
            : "";

    // Severity — from agent report
    const severity =
        report.includes("CRITICAL") ? "CRITICAL" :
        report.includes("HIGH")     ? "HIGH"     : "MEDIUM";

    const sevEl = document.getElementById("report-severity");
    sevEl.textContent = severity;
    sevEl.className = `text-2xl font-bold ${
        severity === "CRITICAL" ? "text-error" :
        severity === "HIGH"     ? "text-tertiary" :
                                  "text-primary"
    }`;

    // Summary — first 3 non-empty lines
    const lines = report.split("\n").map(l => l.trim()).filter(Boolean);
    document.getElementById("report-summary")
        .textContent = lines.slice(0, 3).join(" ");

    // Full report — verbatim
    document.getElementById("report-full").textContent = report;

    // Saved badge
    document.getElementById("saved-badge").classList.remove("hidden");
    document.getElementById("saved-badge").classList.add("flex");

    // ✅ Show source in panel meta
    document.getElementById("panel-meta").innerHTML = `
        <span class="text-[10px] font-bold text-primary bg-primary/10
                     px-2 py-0.5 rounded uppercase tracking-wider">
            ${currentErrorType}
        </span>
        <span class="text-[10px] text-on-surface-variant ml-2 uppercase tracking-wider">
            ${fromCache ? "Loaded from Firestore ⚡" : "Analysis Complete · Saved to Firestore"}
        </span>`;

    // Show footer — just remove hidden, flex-col handles display
    document.getElementById("panel-footer").classList.remove("hidden");

    // Reset PR button
    const prBtn = document.getElementById("pr-btn");
    prBtn.disabled = false;
    prBtn.innerHTML = `
        <span class="material-symbols-outlined text-sm">merge</span>
        Raise Fix PR on GitHub`;

    // Clear old PR link
    document.getElementById("pr-link-container").innerHTML = "";
}

// ── Close panel ───────────────────────────────────
function closePanel() {
    const panel = document.getElementById("diagnostics-panel");
    panel.classList.add("hidden");
    panel.classList.remove("flex");

    document.getElementById("panel-footer").classList.add("hidden");
}