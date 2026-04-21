// ── Raise PR on GitHub ────────────────────────────
async function raisePr() {
    const btn = document.getElementById("pr-btn");
    btn.disabled = true;
    btn.innerHTML = `
        <span class="material-symbols-outlined
                     text-sm analyzing">sync</span>
        Creating PR on GitHub...`;

    document.getElementById("pr-link-container")
        .innerHTML = "";

    try {
        const res = await fetch("/api/raise-pr", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                errorType: currentErrorType,
                report: currentReport
            })
        });

        const data = await res.json();

        if (data.success) {
            btn.innerHTML = `
                <span class="material-symbols-outlined
                             text-sm text-primary">
                    check_circle
                </span>
                PR Created!`;

            let linkHtml = "";

            // Show file that was changed
            if (data.fileChanged) {
                linkHtml += `
                    <div class="text-[10px]
                                text-on-surface-variant
                                text-center mt-1">
                        Changed:
                        <span class="mono-font text-primary">
                            ${data.fileChanged}
                        </span>
                    </div>`;
            }

            // Show PR link
            if (data.prUrl) {
                linkHtml += `
                    <a href="${data.prUrl}"
                       target="_blank"
                       class="w-full flex items-center
                              justify-center gap-1 mt-1
                              text-xs text-primary
                              underline mono-font">
                        <span class="material-symbols-outlined
                                     text-sm">open_in_new</span>
                        View PR on GitHub
                    </a>`;
            }

            document.getElementById("pr-link-container")
                .innerHTML = linkHtml;
        } else {
            btn.disabled = false;
            btn.innerHTML = `
                <span class="material-symbols-outlined
                             text-sm">merge</span>
                Raise Fix PR on GitHub`;
            showToast("PR failed: " + data.error);
        }
    } catch (e) {
        btn.disabled = false;
        btn.innerHTML = `
            <span class="material-symbols-outlined
                         text-sm">merge</span>
            Raise Fix PR on GitHub`;
        showToast("Error: " + e.message);
    }
}