// ── Toast ─────────────────────────────────────────
function showToast(msg) {
    const t = document.getElementById("toast");
    t.textContent = msg;
    t.classList.remove("hidden");
    setTimeout(() => t.classList.add("hidden"), 3000);
}

// ── Scan time ─────────────────────────────────────
function updateScanTime() {
    document.getElementById("scan-time").textContent =
        `Last scan: ${new Date().toLocaleTimeString()}`;
}

// ── Severity badge ────────────────────────────────
function severityBadge(s) {
    if (s === "critical") return `
        <span class="inline-flex items-center gap-1.5
                     px-2 py-0.5 rounded-full
                     bg-error-container
                     text-on-error-container
                     text-[10px] font-bold uppercase">
            <span class="w-1.5 h-1.5 rounded-full
                         bg-error"></span>Critical
        </span>`;
    if (s === "error") return `
        <span class="inline-flex items-center gap-1.5
                     px-2 py-0.5 rounded-full
                     bg-error-container/40
                     text-on-error-container
                     text-[10px] font-bold uppercase">
            <span class="w-1.5 h-1.5 rounded-full
                         bg-error-dim"></span>Error
        </span>`;
    return `
        <span class="inline-flex items-center gap-1.5
                     px-2 py-0.5 rounded-full
                     bg-tertiary-container/20
                     text-tertiary
                     text-[10px] font-bold uppercase">
            <span class="w-1.5 h-1.5 rounded-full
                         bg-tertiary"></span>Warning
        </span>`;
}