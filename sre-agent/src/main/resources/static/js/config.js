// ── Global State ──────────────────────────────────
let liveErrors = [];
let currentErrorType = "";
let currentErrorCount = 0;
let currentErrorLastSeen = "";
let currentReport = "";

// ✅ Map of errorType → incident (from Firestore)
let knownIncidents = {};