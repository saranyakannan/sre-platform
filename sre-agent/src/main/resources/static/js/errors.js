// ── Load incidents from Firestore ─────────────────
async function loadIncidents() {
    try {
        const res = await fetch("/api/incidents");
        const data = await res.json();
        if (data.success) {
            knownIncidents = {};
            (data.incidents || []).forEach(inc => {
                if (inc.errorType) {
                    knownIncidents[inc.errorType] = inc;
                }
            });
        }
        // ✅ Add this temporarily
        console.log("Known incidents:", knownIncidents);
        console.log("Keys:", Object.keys(knownIncidents));
    } catch (e) {
        console.warn("Could not load incidents:", e);
    }
}

// ── Load errors from Cloud Logging ───────────────
async function loadErrors() {
    const days = document.getElementById(
        "filter-time"
    ).value;

    document.getElementById("table-loading")
        .classList.remove("hidden");
    document.getElementById("table-loading")
        .classList.add("flex");
    document.getElementById("table-empty")
        .classList.add("hidden");
    document.getElementById("table-empty")
        .classList.remove("flex");
    document.getElementById("error-table-wrap")
        .classList.add("hidden");
    document.getElementById("event-count")
        .textContent = "Loading...";

    try {
        const res = await fetch(
            `/api/errors?days=${days}`
        );
        const data = await res.json();
        if (data.success) {
            liveErrors = data.errors || [];
        } else {
            liveErrors = [];
            showToast(
                "Could not load errors: " + data.error
            );
        }
    } catch (e) {
        liveErrors = [];
        showToast("Failed to connect: " + e.message);
    }

    updateScanTime();
    renderTable();
}

// ── Render table ──────────────────────────────────
function renderTable() {
    const sevFilter = document.getElementById(
        "filter-severity"
    ).value;

    const filtered = liveErrors.filter(e =>
        sevFilter === "all" || e.severity === sevFilter
    );

    document.getElementById("table-loading")
        .classList.add("hidden");
    document.getElementById("table-loading")
        .classList.remove("flex");

    if (filtered.length === 0) {
        document.getElementById("table-empty")
            .classList.remove("hidden");
        document.getElementById("table-empty")
            .classList.add("flex");
        document.getElementById("error-table-wrap")
            .classList.add("hidden");
        document.getElementById("event-count")
            .textContent = liveErrors.length === 0
                ? "No errors found"
                : "0 match filter";
        return;
    }

    document.getElementById("table-empty")
        .classList.add("hidden");
    document.getElementById("table-empty")
        .classList.remove("flex");
    document.getElementById("error-table-wrap")
        .classList.remove("hidden");
    document.getElementById("event-count")
        .textContent =
            `${filtered.length} error type${
                filtered.length > 1 ? "s" : ""
            }`;

    const tbody = document.getElementById(
        "error-table"
    );
    tbody.innerHTML = filtered.map(e => {
        const lastSeen = e.lastSeen
            ? new Date(e.lastSeen).toLocaleString()
            : "—";
        const safeMsg = (e.msg || "")
            .replace(/'/g, "\\'");
        const safeType = (e.type || "Unknown")
            .replace(/'/g, "\\'");
        const safeLastSeen = (e.lastSeen || "")
            .replace(/'/g, "\\'");

        // ✅ Check if already in Firestore
        const hasIncident =
            knownIncidents[e.type] !== undefined;

        // ✅ Different button based on Firestore state
        const actionBtn = hasIncident
            ? `<button
                onclick="analyzeError('${safeType}','${safeMsg}',${e.count || 1},'${safeLastSeen}')"
                class="text-xs font-semibold
                       text-primary border
                       border-primary/40
                       hover:bg-primary/10
                       px-3 py-1.5 rounded
                       active:scale-95 transition-all
                       flex items-center gap-1 ml-auto">
                <span class="material-symbols-outlined
                             text-sm">visibility</span>
                View Report
               </button>`
            : `<button
                onclick="analyzeError('${safeType}','${safeMsg}',${e.count || 1},'${safeLastSeen}')"
                class="text-xs font-semibold
                       text-on-primary bg-primary
                       hover:opacity-90 px-3 py-1.5
                       rounded active:scale-95
                       transition-all flex items-center
                       gap-1 ml-auto">
                <span class="material-symbols-outlined
                             text-sm">bolt</span>
                Analyze
               </button>`;

        return `
            <tr class="group hover:bg-surface-container-high
                       transition-colors">
                <td class="px-5 py-4">
                    ${severityBadge(e.severity)}
                </td>
                <td class="px-5 py-4">
                    <div class="flex flex-col gap-0.5">
                        <span class="text-sm font-bold
                                     text-on-surface
                                     mono-font">
                            ${e.type || "Unknown"}
                        </span>
                        <span class="text-[11px]
                                     text-on-surface-variant
                                     opacity-70 truncate
                                     max-w-xs">
                            ${e.msg || "—"}
                        </span>
                    </div>
                </td>
                <td class="px-5 py-4 text-right">
                    <span class="text-sm font-bold
                                 mono-font ${
                                     e.count > 10
                                     ? "text-error"
                                     : "text-on-surface"
                                 }">
                        ${e.count || 1}x
                    </span>
                </td>
                <td class="px-5 py-4 text-right">
                    <span class="text-[10px]
                                 text-on-surface-variant
                                 mono-font">
                        ${lastSeen}
                    </span>
                </td>
                <td class="px-5 py-4 text-right">
                    ${actionBtn}
                </td>
            </tr>`;
    }).join("");
}

// ── Severity filter ───────────────────────────────
function applyFilters() {
    renderTable();
}